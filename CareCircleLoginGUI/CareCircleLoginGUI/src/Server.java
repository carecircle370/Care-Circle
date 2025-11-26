import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 1234;
    private static final File CSV_FILE = new File("vitals.csv");

    // groupName -> clients
    private static final Map<String, List<ClientHandler>> chatGroups = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ensureCsvHeader();

        System.out.println("Unified Server listening on port " + PORT);

        ExecutorService pool = Executors.newCachedThreadPool();
        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            pool.submit(() -> {
                try {
                    new ClientHandler(socket, chatGroups).run();
                } catch (Exception e) {
                    // ignore
                }
            });
        }
    }

    // ========== CSV HELPERS ======================================================

    private static void ensureCsvHeader() {
        try {
            boolean newFile = CSV_FILE.createNewFile();
            if (newFile || CSV_FILE.length() == 0L) {
                try (BufferedWriter w = new BufferedWriter(new FileWriter(CSV_FILE, true))) {
                    w.write("patientId,heartRateBpm,bpSystolic,bpDiastolic,temperatureC,mood,dietNotes,weightKg,submittedAt");
                    w.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized void appendCsvLine(String csvLine) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(CSV_FILE, true))) {
            w.write(csvLine);
            w.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendCsv(BufferedWriter bw, String filter) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(CSV_FILE))) {
            String header = r.readLine();
            if (header != null) {
                bw.write(header);
                bw.newLine();
            }

            String row;
            while ((row = r.readLine()) != null) {
                if (filter == null) {
                    bw.write(row);
                    bw.newLine();
                } else {
                    int comma = row.indexOf(',');
                    String pid = (comma == -1) ? row : row.substring(0, comma).trim();
                    if (pid.equalsIgnoreCase(filter)) {
                        bw.write(row);
                        bw.newLine();
                    }
                }
            }
        }

        bw.write("END");
        bw.newLine();
        bw.flush();
    }

    // ========== CLIENT HANDLER ==================================================

    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private final BufferedReader br;
        private final BufferedWriter bw;

        private final Map<String, List<ClientHandler>> groups;

        private String username;
        private String groupName;

        public ClientHandler(Socket socket, Map<String, List<ClientHandler>> groups)
                throws IOException {
            this.socket = socket;
            this.groups = groups;

            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override
        public void run() {
            try {
                handleJoin();

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("CSV|")) {
                        handleCsvCommand(line);
                    } else if (line.equalsIgnoreCase("QUIT")) {
                        send("Goodbye");
                        break;
                    } else {
                        // Normal chat
                        broadcast(username + ": " + line);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                leaveGroup();
                closeEverything();
            }
        }

        // ------- JOIN ----------------------------------------------------------
        private void handleJoin() throws IOException {
            String joinLine = br.readLine();
            if (joinLine == null || !joinLine.startsWith("JOIN|"))
                throw new IOException("Invalid join");

            String[] parts = joinLine.split("\\|");
            if (parts.length < 3)
                throw new IOException("Invalid join");

            groupName = parts[1];
            username = parts[2];

            groups.computeIfAbsent(groupName,
                            k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(this);

            broadcast(username + " has joined the group.");
        }

        // ------- CSV -----------------------------------------------------------
        private void handleCsvCommand(String cmd) throws IOException {

            // CSV|LIST ALL
            if (cmd.equalsIgnoreCase("CSV|LIST ALL")) {
                sendCsv(bw, null);
                return;
            }

            // CSV|LIST <id>
            if (cmd.startsWith("CSV|LIST ")) {
                String id = cmd.substring(9).trim();
                sendCsv(bw, id);
                return;
            }

            // CSV|SUBMIT|<data>
            if (cmd.startsWith("CSV|SUBMIT|")) {
                String data = cmd.substring(11);
                String timestamped = data + "," + Instant.now();
                appendCsvLine(timestamped);
                send("CSV OK");
                return;
            }

            send("CSV ERROR: Unknown command");
        }

        // ------- CHAT HELPERS --------------------------------------------------
        private void broadcast(String msg) {
            List<ClientHandler> list = groups.get(groupName);
            if (list == null) return;

            synchronized (list) {
                for (ClientHandler c : list)
                    c.send(msg);
            }
        }

        private void leaveGroup() {
            List<ClientHandler> list = groups.get(groupName);
            if (list != null) {
                list.remove(this);
                broadcast(username + " has left the group.");
            }
        }

        private void send(String msg) {
            try {
                bw.write(msg);
                bw.newLine();
                bw.flush();
            } catch (IOException ignored) {}
        }

        private void closeEverything() {
            try { br.close(); } catch (IOException ignored) {}
            try { bw.close(); } catch (IOException ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}