package com.carecircle.core;

import com.carecircle.data.CalendarDTO;
import com.carecircle.data.CalendarStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central hub for appointment services, scoping, provider access control,
 * factories, and CSV utilities.
 */
public final class Dispatchers {

    // -------- Appointments API (kept nested) --------
    public interface CalendarDispatch {
        boolean bookAppointment(CalendarDTO dto);
        boolean cancelAppointment(UUID id);
        List<CalendarDTO> listAppointmentsByPatient(String patientId);
        List<CalendarDTO> listAllAppointments();
    }

    // -------- Scoping --------
    public static final class SessionScope {
        public enum Role { PATIENT, PROVIDER }
        private final Role role;
        private final String patientId;
        private final String providerId;

        private SessionScope(Role role, String patientId, String providerId) {
            this.role = Objects.requireNonNull(role);
            this.patientId = patientId;
            this.providerId = providerId;
            if (role == Role.PATIENT && (patientId == null || patientId.isBlank()))
                throw new IllegalArgumentException("PATIENT scope requires patientId");
            if (role == Role.PROVIDER && (providerId == null || providerId.isBlank()))
                throw new IllegalArgumentException("PROVIDER scope requires providerId");
        }
        public static SessionScope forPatient(String patientId){ return new SessionScope(Role.PATIENT, patientId, null); }
        public static SessionScope forProvider(String providerId){ return new SessionScope(Role.PROVIDER, null, providerId); }
        public Role role(){ return role; }
        public String patientId(){ return patientId; }
        public String providerId(){ return providerId; }
    }

    // -------- Provider access control (CSV) --------
    public interface ProviderAccessControl {
        boolean canAccess(String providerId, String patientId);
        Set<String> patientsFor(String providerId);
        void assign(String providerId, String patientId);
        void unassign(String providerId, String patientId);
    }

    static final class CsvProviderAccessControl implements ProviderAccessControl {
        private static final String[] HEADER = {"providerId","patientId"};
        private final Path file;
        private final ConcurrentHashMap<String, Set<String>> map = new ConcurrentHashMap<>();

        CsvProviderAccessControl(File f) { this.file = f.toPath(); load(); }

        @Override public boolean canAccess(String providerId, String patientId) {
            Set<String> s = map.get(providerId);
            return providerId != null && patientId != null && s != null && s.contains(patientId);
        }
        @Override public Set<String> patientsFor(String providerId) {
            Set<String> s = map.get(providerId);
            return s == null ? Set.of() : Collections.unmodifiableSet(s);
        }
        @Override
        public synchronized void assign(String providerId, String patientId) {
            if (blank(providerId) || blank(patientId)) return;
            map.computeIfAbsent(providerId, k -> ConcurrentHashMap.newKeySet()).add(patientId);
            persist();
        }
        @Override public synchronized void unassign(String providerId, String patientId) {
            if (blank(providerId) || blank(patientId)) return;
            Set<String> s = map.get(providerId);
            if (s != null) { s.remove(patientId); if (s.isEmpty()) map.remove(providerId); persist(); }
        }

        private void load() {
            try {
                ensureFile();
                try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    br.readLine(); // header
                    String line;
                    while ((line = br.readLine()) != null) {
                        var c = csvSplit(line);
                        if (c.size() >= 2) {
                            String prov = c.get(0);
                            String pid = c.get(1);
                            if (!blank(prov) && !blank(pid)) {
                                map.computeIfAbsent(prov, k -> ConcurrentHashMap.newKeySet()).add(pid);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        private void persist() {
            try {
                ensureParent();
                try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    bw.write(csvJoin(HEADER)); bw.newLine();
                    for (var e : map.entrySet())
                        for (String pid : e.getValue()) { bw.write(csvJoin(e.getKey(), pid)); bw.newLine(); }
                }
            } catch (IOException ignored) {}
        }
        private void ensureFile() throws IOException {
            ensureParent();
            if (!Files.exists(file)) {
                try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                    bw.write(csvJoin(HEADER)); bw.newLine();
                }
            }
        }
        private void ensureParent() throws IOException {
            Path p = file.getParent();
            if (p != null && !Files.exists(p)) Files.createDirectories(p);
        }
        private static boolean blank(String s){ return s==null || s.isBlank(); }
    }

    // -------- Appointments impls --------
    static final class AppointmentManager implements CalendarDispatch {
        private final CalendarStore store;
        AppointmentManager(CalendarStore store){ this.store = Objects.requireNonNull(store); }
        @Override public boolean bookAppointment(CalendarDTO dto){ return store.save(dto) != null; }
        @Override public boolean cancelAppointment(UUID id){ return store.deleteById(id); }
        @Override public List<CalendarDTO> listAppointmentsByPatient(String patientId){ return store.findByPatient(patientId); }
        @Override public List<CalendarDTO> listAllAppointments(){ return store.findAll(); }
    }

    static final class ScopedCalendarDispatch implements CalendarDispatch {
        private final CalendarDispatch target;
        private final SessionScope scope;
        private final ProviderAccessControl pac;

        ScopedCalendarDispatch(CalendarDispatch t, SessionScope s, ProviderAccessControl p){ this.target=t; this.scope=s; this.pac=p; }

        @Override public boolean bookAppointment(CalendarDTO dto) {
            return switch (scope.role()) {
                case PATIENT  -> scope.patientId().equalsIgnoreCase(dto.patientId()) && target.bookAppointment(dto);
                case PROVIDER -> (pac == null || pac.canAccess(scope.providerId(), dto.patientId())) && target.bookAppointment(dto);
            };
        }
        @Override public boolean cancelAppointment(UUID id) {
            if (id == null) return false;
            return switch (scope.role()) {
                case PATIENT -> target.listAppointmentsByPatient(scope.patientId()).stream().anyMatch(a -> id.equals(a.id()))
                        && target.cancelAppointment(id);
                case PROVIDER -> {
                    if (pac == null) yield target.cancelAppointment(id);
                    var allowed = pac.patientsFor(scope.providerId());
                    boolean mine = target.listAllAppointments().stream()
                            .filter(a -> allowed.contains(a.patientId()))
                            .anyMatch(a -> id.equals(a.id()));
                    yield mine && target.cancelAppointment(id);
                }
            };
        }
        @Override public List<CalendarDTO> listAppointmentsByPatient(String patientId) {
            return switch (scope.role()) {
                case PATIENT  -> target.listAppointmentsByPatient(scope.patientId());
                case PROVIDER -> (pac != null && !pac.canAccess(scope.providerId(), patientId)) ? List.of()
                        : target.listAppointmentsByPatient(patientId);
            };
        }
        @Override public List<CalendarDTO> listAllAppointments() {
            return switch (scope.role()) {
                case PATIENT  -> target.listAppointmentsByPatient(scope.patientId());
                case PROVIDER -> {
                    if (pac == null) yield target.listAllAppointments();
                    var allowed = pac.patientsFor(scope.providerId());
                    yield target.listAllAppointments().stream()
                            .filter(a -> allowed.contains(a.patientId()))
                            .collect(Collectors.toList());
                }
            };
        }
    }

    // -------- Vitals impls (now use top-level interfaces/classes) --------
    static final class CsvVitalsService implements VitalsDispatch {
        private final File csv;
        CsvVitalsService(File csv){ this.csv = Objects.requireNonNull(csv); }

        @Override public List<VitalsRecord> listByPatient(String patientId) {
            if (patientId == null || patientId.isBlank()) return List.of();
            return filter(pid -> pid.equalsIgnoreCase(patientId));
        }
        @Override public List<VitalsRecord> listAll(){ return filter(pid -> true); }

        private List<VitalsRecord> filter(java.util.function.Predicate<String> pidPred) {
            List<VitalsRecord> out = new ArrayList<>();
            if (!csv.exists()) return out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8))) {
                br.readLine(); // header
                String line;
                while ((line = br.readLine()) != null) {
                    var cols = csvSplit(line);
                    if (cols.isEmpty()) continue;
                    String pid = cols.get(0);
                    if (!pidPred.test(pid)) continue;
                    out.add(VitalsRecord.fromCsv(cols));
                }
            } catch (IOException ignored) {}
            return out;
        }
    }

    static final class ScopedVitalsDispatch implements VitalsDispatch {
        private final VitalsDispatch target;
        private final SessionScope scope;
        private final ProviderAccessControl pac;
        ScopedVitalsDispatch(VitalsDispatch t, SessionScope s, ProviderAccessControl p){ this.target=t; this.scope=s; this.pac=p; }

        @Override public List<VitalsRecord> listByPatient(String patientId) {
            return switch (scope.role()) {
                case PATIENT  -> target.listByPatient(scope.patientId());
                case PROVIDER -> (pac != null && !pac.canAccess(scope.providerId(), patientId)) ? List.of()
                        : target.listByPatient(patientId);
            };
        }
        @Override public List<VitalsRecord> listAll() {
            return switch (scope.role()) {
                case PATIENT  -> target.listByPatient(scope.patientId());
                case PROVIDER -> {
                    if (pac == null) yield target.listAll();
                    var allowed = pac.patientsFor(scope.providerId());
                    yield target.listAll().stream().filter(v -> allowed.contains(v.patientId())).collect(Collectors.toList());
                }
            };
        }
    }

    // -------- Factories --------
    public static final class Factory {
        private static final File APPTS  = new File("appointments.csv");
        private static final File VITALS = new File("vitals.csv");
        private static final File ACCESS = new File("provider_access.csv");

        private static volatile CalendarDispatch ROOT_CAL;
        private static volatile VitalsDispatch ROOT_VIT;
        private static volatile ProviderAccessControl PAC;

        private Factory(){}

        private static CalendarDispatch rootCalendar() {
            if (ROOT_CAL == null) synchronized (Factory.class) {
                if (ROOT_CAL == null) ROOT_CAL = new AppointmentManager(new CalendarStore(APPTS));
            }
            return ROOT_CAL;
        }
        private static VitalsDispatch rootVitals() {
            if (ROOT_VIT == null) synchronized (Factory.class) {
                if (ROOT_VIT == null) ROOT_VIT = new CsvVitalsService(VITALS);
            }
            return ROOT_VIT;
        }
        public static ProviderAccessControl accessControl() {
            if (PAC == null) synchronized (Factory.class) {
                if (PAC == null) PAC = new CsvProviderAccessControl(ACCESS);
            }
            return PAC;
        }

        // Patient-scoped
        public static CalendarDispatch calendarForPatient(String patientId) {
            return new ScopedCalendarDispatch(rootCalendar(), SessionScope.forPatient(patientId), accessControl());
        }
        public static VitalsDispatch vitalsForPatient(String patientId) {
            return new ScopedVitalsDispatch(rootVitals(), SessionScope.forPatient(patientId), accessControl());
        }

        // Provider-scoped
        public static CalendarDispatch calendarForProvider(String providerId) {
            return new ScopedCalendarDispatch(rootCalendar(), SessionScope.forProvider(providerId), accessControl());
        }
        public static VitalsDispatch vitalsForProvider(String providerId) {
            return new ScopedVitalsDispatch(rootVitals(), SessionScope.forProvider(providerId), accessControl());
        }
    }

    // -------- CSV utils (public) --------
    public static String csvJoin(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String s = fields[i] == null ? "" : fields[i];
            boolean q = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
            s = s.replace("\"", "\"\"");
            sb.append(q ? "\"" + s + "\"" : s);
            if (i < fields.length - 1) sb.append(',');
        }
        return sb.toString();
    }
    public static List<String> csvSplit(String line) {
        List<String> out = new ArrayList<>();
        if (line == null || line.isEmpty()) return out;
        int i = 0, n = line.length();
        while (i < n) {
            if (line.charAt(i) == '"') {
                i++; StringBuilder cell = new StringBuilder();
                while (i < n) {
                    char c = line.charAt(i++);
                    if (c == '"') {
                        if (i < n && line.charAt(i) == '"') { cell.append('"'); i++; }
                        else break;
                    } else cell.append(c);
                }
                out.add(cell.toString());
                if (i < n && line.charAt(i) == ',') i++;
            } else {
                int j = i;
                while (j < n && line.charAt(j) != ',') j++;
                out.add(line.substring(i, j));
                i = j + 1;
            }
        }
        return out;
    }

    private Dispatchers() {}
}
