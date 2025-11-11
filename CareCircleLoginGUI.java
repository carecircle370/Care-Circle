import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CareCircleLoginGUI extends JFrame {

    // Store users in a simple CSV file: username,pin,role,name
    private static final String USER_FILE = "users.txt"; 

    // CardLayout lets us switch between Login and Sign Up screens
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // Readable fonts for older users
    private final Font H1 = new Font("SansSerif", Font.BOLD, 22);
    private final Font H2 = new Font("SansSerif", Font.PLAIN, 18);
    private final Font BTN = new Font("SansSerif", Font.BOLD, 16);

    public CareCircleLoginGUI() {
        setTitle("Care Circle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        root.add(buildLoginPanel(), "login");
        root.add(buildSignupPanel(), "signup");
        add(root);

        ensureUserFile();
    }

    // ---------- Login Panel ----------
    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Welcome to Care Circle");
        title.setFont(H1);

        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(H2);
        JTextField userField = new JTextField(20);
        userField.setFont(H2);

        JLabel pinLbl = new JLabel("4-digit PIN:");
        pinLbl.setFont(H2);
        JPasswordField pinField = new JPasswordField(20);
        pinField.setFont(H2);
        pinField.setEchoChar('•');

        JLabel msg = new JLabel(" ");
        msg.setFont(H2);
        msg.setForeground(new Color(150, 0, 0));

        JButton loginBtn = new JButton("Log In");
        loginBtn.setFont(BTN);

        JButton goSignup = new JButton("Create New Account");
        goSignup.setFont(BTN);

        // layout
        c.gridy = 0; c.insets = new Insets(0,0,12,0); p.add(title, c);
        c.gridy++; c.insets = new Insets(8,0,4,0); p.add(userLbl, c);
        c.gridy++; c.insets = new Insets(0,0,8,0); p.add(userField, c);
        c.gridy++; c.insets = new Insets(8,0,4,0); p.add(pinLbl, c);
        c.gridy++; c.insets = new Insets(0,0,8,0); p.add(pinField, c);
        c.gridy++; c.insets = new Insets(8,0,0,0); p.add(loginBtn, c);
        c.gridy++; c.insets = new Insets(8,0,0,0); p.add(goSignup, c);
        c.gridy++; c.insets = new Insets(12,0,0,0); p.add(msg, c);

        // actions
        loginBtn.addActionListener((ActionEvent e) -> {
            String u = userField.getText().trim();
            String pin = new String(pinField.getPassword()).trim();

            if (u.isEmpty()) { setError(msg, "Please enter username."); return; }
            if (!isValidPin(pin)) { setError(msg, "PIN must be exactly 4 digits."); return; }

            User user = findUser(u, pin);
            if (user != null) {
                setSuccess(msg, "Login successful. Hello " + user.name + " (" + user.role + ")!");
                JOptionPane.showMessageDialog(this,
                        "Welcome, " + user.name + " (" + user.role + ")!",
                        "Logged In", JOptionPane.INFORMATION_MESSAGE);
                userField.setText("");
                pinField.setText("");
            } else {
                setError(msg, "Invalid username or PIN.");
            }
        });

        goSignup.addActionListener(e -> cards.show(root, "signup"));

        return p;
    }

    // ---------- Sign Up Panel ----------
    private JPanel buildSignupPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Create Account");
        title.setFont(H1);

        JLabel nameLbl = new JLabel("Full name:");
        nameLbl.setFont(H2);
        JTextField nameField = new JTextField(20);
        nameField.setFont(H2);

        JLabel userLbl = new JLabel("Choose username:");
        userLbl.setFont(H2);
        JTextField userField = new JTextField(20);
        userField.setFont(H2);

        JLabel roleLbl = new JLabel("Role:");
        roleLbl.setFont(H2);
        String[] roles = {"patient", "caregiver", "family", "doctor"};
        JComboBox<String> roleBox = new JComboBox<>(roles);
        roleBox.setFont(H2);

        JLabel pinLbl = new JLabel("4-digit PIN:");
        pinLbl.setFont(H2);
        JPasswordField pinField = new JPasswordField(20);
        pinField.setFont(H2);
        pinField.setEchoChar('•');

        JLabel pin2Lbl = new JLabel("Confirm PIN:");
        pin2Lbl.setFont(H2);
        JPasswordField pin2Field = new JPasswordField(20);
        pin2Field.setFont(H2);
        pin2Field.setEchoChar('•');

        JLabel msg = new JLabel(" ");
        msg.setFont(H2);
        msg.setForeground(new Color(150, 0, 0));

        JButton createBtn = new JButton("Create Account");
        createBtn.setFont(BTN);

        JButton backBtn = new JButton("Back to Login");
        backBtn.setFont(BTN);

        // layout
        int y = 0;
        c.gridy = y++; c.insets = new Insets(0,0,12,0); p.add(title, c);

        c.gridy = y++; c.insets = new Insets(6,0,3,0); p.add(nameLbl, c);
        c.gridy = y++; c.insets = new Insets(0,0,6,0); p.add(nameField, c);

        c.gridy = y++; c.insets = new Insets(6,0,3,0); p.add(userLbl, c);
        c.gridy = y++; c.insets = new Insets(0,0,6,0); p.add(userField, c);

        c.gridy = y++; c.insets = new Insets(6,0,3,0); p.add(roleLbl, c);
        c.gridy = y++; c.insets = new Insets(0,0,6,0); p.add(roleBox, c);

        c.gridy = y++; c.insets = new Insets(6,0,3,0); p.add(pinLbl, c);
        c.gridy = y++; c.insets = new Insets(0,0,6,0); p.add(pinField, c);

        c.gridy = y++; c.insets = new Insets(6,0,3,0); p.add(pin2Lbl, c);
        c.gridy = y++; c.insets = new Insets(0,0,6,0); p.add(pin2Field, c);

        c.gridy = y++; c.insets = new Insets(8,0,0,0); p.add(createBtn, c);
        c.gridy = y++; c.insets = new Insets(8,0,0,0); p.add(backBtn, c);

        c.gridy = y++; c.insets = new Insets(12,0,0,0); p.add(msg, c);

        // actions
        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = userField.getText().trim().toLowerCase(Locale.ROOT);
            String role = ((String) roleBox.getSelectedItem()).toLowerCase(Locale.ROOT);
            String pin = new String(pinField.getPassword()).trim();
            String pin2 = new String(pin2Field.getPassword()).trim();

            if (name.isEmpty()) { setError(msg, "Please enter full name."); return; }
            if (username.isEmpty()) { setError(msg, "Please choose a username."); return; }
            if (!isValidPin(pin)) { setError(msg, "PIN must be exactly 4 digits."); return; }
            if (!pin.equals(pin2)) { setError(msg, "PINs do not match."); return; }
            if (userExists(username)) { setError(msg, "Username already exists."); return; }

            if (saveUser(username, pin, role, name)) {
                setSuccess(msg, "Account created! You can log in now.");
                nameField.setText("");
                userField.setText("");
                pinField.setText("");
                pin2Field.setText("");
                roleBox.setSelectedIndex(0);

                int go = JOptionPane.showConfirmDialog(this,
                        "Account created. Go to Login?",
                        "Success",
                        JOptionPane.YES_NO_OPTION);
                if (go == JOptionPane.YES_OPTION) cards.show(root, "login");
            } else {
                setError(msg, "Could not save account (disk error).");
            }
        });

        backBtn.addActionListener(e -> cards.show(root, "login"));

        return p;
    }

    // ---------- Helpers ----------
    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        return c;
    }

    private void setError(JLabel label, String text) {
        label.setForeground(new Color(150, 0, 0));
        label.setText(text);
    }

    private void setSuccess(JLabel label, String text) {
        label.setForeground(new Color(0, 120, 0));
        label.setText(text);
    }

    private static boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 4) return false;
        for (char ch : pin.toCharArray()) if (!Character.isDigit(ch)) return false;
        return true;
    }

    private static void ensureUserFile() {
        try {
            Path path = Path.of(USER_FILE);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException ignored) { }
    }

    private static boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",", 4);
                if (v.length >= 1 && v[0].equalsIgnoreCase(username)) return true;
            }
        } catch (IOException ignored) { }
        return false;
    }

    private static boolean saveUser(String username, String pin, String role, String name) {
        try (FileWriter fw = new FileWriter(USER_FILE, true)) {
            fw.write(username + "," + pin + "," + role + "," + name + System.lineSeparator());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static User findUser(String username, String pin) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",", 4); // username,pin,role,name
                if (v.length >= 4) {
                    if (v[0].equalsIgnoreCase(username) && v[1].equals(pin)) {
                        return new User(v[0], v[1], v[2], v[3]);
                    }
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    private record User(String username, String pin, String role, String name) {}

    // ---------- Main ----------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new CareCircleLoginGUI().setVisible(true));
    }
}
