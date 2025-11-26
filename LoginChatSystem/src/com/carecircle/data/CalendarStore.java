package com.carecircle.data;

import com.carecircle.core.Dispatchers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/** CSV-backed DAO for appointments (thread-safe, header-safe). */
public final class CalendarStore {
    private final File file;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    public CalendarStore(File file) {
        this.file = file;
        ensureHeader();
    }

    public UUID save(CalendarDTO dto) {
        if (dto == null) return null;
        CalendarDTO toWrite = dto.id() == null
                ? CalendarDTO.newFromUI(dto.patientId(), dto.patientName(), dto.professionalName(),
                dto.professionalType(), dto.appointmentTime(), dto.reason(), dto.durationMinutes())
                : dto;
        rw.writeLock().lock();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            bw.write(toWrite.toCsvLine()); bw.newLine();
            return toWrite.id();
        } catch (IOException e) {
            return null;
        } finally { rw.writeLock().unlock(); }
    }

    public boolean deleteById(UUID id) {
        if (id == null) return false;
        rw.writeLock().lock();
        try {
            List<CalendarDTO> all = findAll();
            List<CalendarDTO> remaining = all.stream().filter(a -> !id.equals(a.id())).collect(Collectors.toList());
            if (remaining.size() == all.size()) return false;
            writeAll(remaining);
            return true;
        } finally { rw.writeLock().unlock(); }
    }

    public List<CalendarDTO> findByPatient(String patientId) {
        if (patientId == null || patientId.isBlank()) return List.of();
        return findAll().stream().filter(a -> patientId.equalsIgnoreCase(a.patientId())).collect(Collectors.toList());
    }

    public List<CalendarDTO> findAll() {
        rw.readLock().lock();
        List<CalendarDTO> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try { out.add(CalendarDTO.fromCsvLine(line)); } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
        finally { rw.readLock().unlock(); }
        return out;
    }

    private void writeAll(List<CalendarDTO> items) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            bw.write(Dispatchers.csvJoin(CalendarDTO.HEADER)); bw.newLine();
            for (CalendarDTO a : items) { bw.write(a.toCsvLine()); bw.newLine(); }
        } catch (IOException ignored) {}
    }

    private void ensureHeader() {
        if (file.exists() && file.length() > 0) return;
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            bw.write(Dispatchers.csvJoin(CalendarDTO.HEADER)); bw.newLine();
        } catch (IOException ignored) {}
    }
}