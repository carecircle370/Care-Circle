import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {

    private JTextArea chatArea;
    private JTextField textInput;
    private JButton sendButton;
    private String username;

    private Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;

    public Client(String username, String serverAddress, int port) {
        this.username = username;

        setTitle("TEST CHATBOX - User: " + username);
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 20));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        textInput = new JTextField();
        textInput.setFont(new Font("Arial", Font.PLAIN, 20));

        sendButton = new JButton(">>");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 20));

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(textInput, BorderLayout.CENTER);
        contentPane.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(contentPane, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        textInput.addActionListener(e -> sendMessage());

        try {
            socket = new Socket(serverAddress, port);
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            bw.write(username);
            bw.newLine();
            bw.flush();

            listenForMessages();

        } catch (IOException e) {
            showMessage("Error connecting to server.");
            closeEverything();
        }

        setVisible(true);
    }

    private void sendMessage() {
        String message = textInput.getText().trim();
        if (!message.isEmpty()) {
            send("CHAT", message);
            textInput.setText("");
        }
    }

    private void listenForMessages() {
        Thread thread = new Thread(() -> {
            String msgFromServer;
            try {
                while ((msgFromServer = br.readLine()) != null) {
                    parseAndDisplayMessage(msgFromServer);
                }
            } catch (IOException e) {
                showMessage("Connection closed.");
                closeEverything();
            }
        });
        thread.start();
    }

    private void parseAndDisplayMessage(String rawMessage) { // OPCODE|USER|MESSAGE
        String[] parts = rawMessage.split("\\|", 3);

        String opcode = parts[0];
        String sender = parts[1];
        String message = parts[2];

        switch (opcode) {
            case "CHAT":
                showMessage(sender + ": " + message);
                break;

            case "SERVER":
                showMessage("[SERVER] " + message);
                break;

            default:
                showMessage("[UNKNOWN OPCODE] " + rawMessage);
                break;
        }
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
        });
    }

    private void closeEverything() {
        try {
            if (br != null) br.close();
            if (bw != null) bw.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String opcode, String data) {
        try {
            String message = opcode + "|" + username + "|" + data;
            bw.write(message);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            showMessage("Error sending message: " + e.getMessage());
            closeEverything();
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username (TEMPORARY):");
        if (username != null) {
            new Client(username, "localhost", 1234);
        } else {
            System.out.println("Username cannot be empty.");
        }
    }
}

