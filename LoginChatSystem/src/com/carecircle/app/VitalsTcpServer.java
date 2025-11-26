// ============================================================================
// File: src/com/carecircle/app/VitalsTcpServer.java
// ============================================================================
package com.carecircle.app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** TCP server saving vitals into vitals.csv (now includes patientName). */
public final class VitalsTcpServer {
    private static final int PORT = 1234;
    private static final File CSV = new File("vitals.csv");

    public static void main(String[] args) {
        System.out.println("Vitals server listening on port " + PORT);
        ensureHeader();
        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket client = ss.accept();
                new Thread(() -> handle(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handle(Socket s) {
        try (s;
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null) return;
            String trimmed = line.trim();
            if ("QUIT".equalsIgnoreCase(trimmed)) {
                bw.write("Goodbye"); bw.newLine(); bw.flush(); return;
            }
            if ("LIST ALL".equalsIgnoreCase(trimmed)) { streamCsv(bw, null); return; }
            if (trimmed.toUpperCase().startsWith("LIST ")) {
                String pid = trimmed.substring(5).trim();
                streamCsv(bw, pid.isEmpty()?null:pid); return;
            }
            append(line + "," + Instant.now());
            bw.write("OK saved to vitals.csv"); bw.newLine(); bw.flush();
        } catch (IOException ignored) {}
    }

    private static synchronized void append(String csvLine) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV, true), StandardCharsets.UTF_8))) {
            bw.write(csvLine); bw.newLine();
        } catch (IOException ignored) {}
    }

    private static void streamCsv(BufferedWriter bw, String filterPid) throws IOException {
        ensureHeader();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CSV), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header != null) { bw.write(header); bw.newLine(); }
            String row;
            while ((row = br.readLine()) != null) {
                if (filterPid == null) { bw.write(row); bw.newLine(); }
                else {
                    int idx = row.indexOf(',');
                    String pid = idx < 0 ? row : row.substring(0, idx);
                    if (filterPid.equalsIgnoreCase(pid.trim())) { bw.write(row); bw.newLine(); }
                }
            }
        } catch (IOException ex) {
            bw.write("ERROR: " + ex.getMessage()); bw.newLine();
        }
        bw.write("END"); bw.newLine(); bw.flush();
    }

    private static void ensureHeader() {
        if (CSV.exists() && CSV.length() > 0) return;
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV, true), StandardCharsets.UTF_8))) {
            // NEW header with patientName as column 2
            bw.write("patientId,patientName,heartRateBpm,bpSystolic,bpDiastolic,temperatureC,mood,dietNotes,weightKg,submittedAt");
            bw.newLine();
        } catch (IOException ignored) {}
    }
}
