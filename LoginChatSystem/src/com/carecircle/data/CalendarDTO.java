package com.carecircle.data;

import com.carecircle.core.Dispatchers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Appointment record with CSV helpers. */
public record CalendarDTO(
        UUID id,
        String patientId,
        String patientName,
        String professionalName,
        String professionalType,
        LocalDateTime appointmentTime,
        String reason,
        int durationMinutes,
        Instant createdAt
) {
    public static final String[] HEADER = {
            "id","patientId","patientName","professionalName","professionalType",
            "appointmentTimeISO","reason","durationMinutes","createdAt"
    };

    public static CalendarDTO newFromUI(String patientId, String patientName,
                                        String professionalName, String professionalType,
                                        LocalDateTime at, String reason, int durationMinutes) {
        return new CalendarDTO(UUID.randomUUID(), patientId, patientName, professionalName, professionalType, at, reason, durationMinutes, Instant.now());
    }

    public String toCsvLine() {
        return Dispatchers.csvJoin(
                id == null ? "" : id.toString(),
                nz(patientId), nz(patientName), nz(professionalName), nz(professionalType),
                appointmentTime == null ? "" : appointmentTime.toString(),
                nz(reason),
                Integer.toString(durationMinutes),
                createdAt == null ? "" : createdAt.toString()
        );
    }

    public static CalendarDTO fromCsvLine(String line) {
        List<String> c = Dispatchers.csvSplit(line);
        UUID id = parseUuid(g(c,0));
        String patientId = g(c,1);
        String patientName = g(c,2);
        String professionalName = g(c,3);
        String professionalType = g(c,4);
        LocalDateTime at = parseDateTime(g(c,5));
        String reason = g(c,6);
        int duration = parseInt(g(c,7));
        Instant created = parseInstant(g(c,8));
        return new CalendarDTO(id, patientId, patientName, professionalName, professionalType, at, reason, duration, created);
    }

    private static String g(List<String> c,int i){ return (i>=0 && i<c.size())?c.get(i):""; }
    private static String nz(String s){ return s==null?"":s; }
    private static int parseInt(String s){ try{ return Integer.parseInt(s.trim()); }catch(Exception e){ return 0; } }
    private static UUID parseUuid(String s){ try{ return (s==null||s.isBlank())?null:UUID.fromString(s.trim()); }catch(Exception e){ return null; } }
    private static LocalDateTime parseDateTime(String s){ try{ return (s==null||s.isBlank())?null:LocalDateTime.parse(s.trim()); }catch(Exception e){ return null; } }
    private static Instant parseInstant(String s){ try{ return (s==null||s.isBlank())?null:Instant.parse(s.trim()); }catch(Exception e){ return null; } }
}