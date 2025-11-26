import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader br;
    private final BufferedWriter bw;
    private final Map<String, List<ClientHandler>> chatGroups;

    private String username;
    private String groupName;

    public ClientHandler(Socket socket, String username, String groupName,
                         Map<String, List<ClientHandler>> chatGroups) throws IOException {
        this.socket = socket;
        this.username = username;
        this.groupName = groupName;
        this.chatGroups = chatGroups;

        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        chatGroups.computeIfAbsent(groupName, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(this);

        broadcastToGroup(username + " has joined the group.");
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = br.readLine()) != null) {
                broadcastToGroup(username + ": " + message);
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected from " + groupName);
        } finally {
            removeClient();
            closeEverything();
        }
    }

    private void broadcastToGroup(String message) {
        List<ClientHandler> clients = chatGroups.get(groupName);
        synchronized (clients) {
            for (ClientHandler c : clients) {
                try {
                    c.bw.write(message);
                    c.bw.newLine();
                    c.bw.flush();
                } catch (IOException ignored) {}
            }
        }
    }

    private void removeClient() {
        List<ClientHandler> clients = chatGroups.get(groupName);
        clients.remove(this);
        broadcastToGroup(username + " has left the group.");
    }

    private void closeEverything() {
        try { if (br != null) br.close(); if (bw != null) bw.close(); if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}