package com.carecircle.core;

import java.time.Instant;
import java.util.List;

public record VitalsRecord(
        String patientId,
        String patientName,       // can be blank for old rows
        Integer heartRateBpm,
        Integer bpSystolic,
        Integer bpDiastolic,
        Double temperatureC,
        String mood,
        String dietNotes,
        Double weightKg,
        Instant submittedAt
) {
    public static VitalsRecord fromCsv(List<String> c) {
        // NEW format (10 cols): id,name,hr,sys,dia,temp,mood,diet,kg,ts
        if (c.size() >= 10) {
            return new VitalsRecord(
                    g(c,0), g(c,1),
                    pInt(g(c,2)), pInt(g(c,3)), pInt(g(c,4)),
                    pDbl(g(c,5)), g(c,6), g(c,7), pDbl(g(c,8)),
                    pInst(g(c,9))
            );
        }
        // OLD format (9 cols): id,hr,sys,dia,temp,mood,diet,kg,ts
        return new VitalsRecord(
                g(c,0), "",
                pInt(g(c,1)), pInt(g(c,2)), pInt(g(c,3)),
                pDbl(g(c,4)), g(c,5), g(c,6), pDbl(g(c,7)),
                pInst(g(c,8))
        );
    }

    private static String g(List<String> c,int i){ return (i>=0 && i<c.size())?c.get(i):""; }
    private static Integer pInt(String s){ try{ return (s==null||s.isBlank())?null:Integer.valueOf(s.trim()); }catch(Exception e){ return null; } }
    private static Double  pDbl(String s){ try{ return (s==null||s.isBlank())?null:Double.valueOf(s.trim()); }catch(Exception e){ return null; } }
    private static Instant pInst(String s){ try{ return (s==null||s.isBlank())?null:Instant.parse(s.trim()); }catch(Exception e){ return null; } }
}
