import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Chat extends JFrame {

    private JTextArea chatArea;
    private JTextField textInput;
    private JButton sendButton;

    private String username;
    private String groupName;

    private Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Chat(String username, String serverAddress, int port) {
        this.username = username;

        // Prompt for group
        this.groupName = JOptionPane.showInputDialog("Enter group name:");
        if (groupName == null || groupName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Group name cannot be empty. Closing chat.");
            return;
        }

        setTitle("Chat - User: " + username + " | Group: " + groupName);
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Only closes this window
        setLayout(new BorderLayout());

        // Chat display
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 18));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Input area
        textInput = new JTextField();
        textInput.setFont(new Font("Arial", Font.PLAIN, 18));
        sendButton = new JButton(">>");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 18));

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(textInput, BorderLayout.CENTER);
        contentPane.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(contentPane, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        textInput.addActionListener(e -> sendMessage());

        // Connect to server
        try {
            socket = new Socket(serverAddress, port);
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send group + username as JOIN message
            bw.write("JOIN|" + groupName + "|" + username);
            bw.newLine();
            bw.flush();

            listenForMessages();
        } catch (IOException e) {
            showMessage("[ERROR] Unable to connect to server: " + e.getMessage());
            closeEverything();
        }

        setVisible(true);
    }

    private void sendMessage() {
        String message = textInput.getText().trim();
        if (!message.isEmpty()) {
            try {
                bw.write(message); // only send the actual message, no username
                bw.newLine();
                bw.flush();

                textInput.setText("");
            } catch (IOException e) {
                showMessage("[ERROR] Failed to send message: " + e.getMessage());
                closeEverything();
            }
        }
    }

    private void listenForMessages() {
        Thread thread = new Thread(() -> {
            String msgFromServer;
            try {
                while ((msgFromServer = br.readLine()) != null) {
                    // Add timestamp and display
                    String timestamp = dtf.format(LocalDateTime.now());
                    showMessage("[" + timestamp + "] " + msgFromServer);
                }
            } catch (IOException e) {
                showMessage("[INFO] Connection closed.");
                closeEverything();
            }
        });
        thread.start();
    }

    private String formatMessage(String sender, String message) {
        String timestamp = dtf.format(LocalDateTime.now());
        return "[" + timestamp + "] " + sender + ": " + message;
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
        logMessage(groupName, message);
    }

    private void logMessage(String groupName, String message) {
        if (groupName == null || groupName.isEmpty()) return;
        String logFileName = groupName + "_chat_log.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write chat log for group " + groupName + ": " + e.getMessage());
        }
    }

    private void closeEverything() {
        try {
            if (br != null) br.close();
            if (bw != null) bw.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username == null || username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        new Chat(username, "localhost", 1000);
    }
}

