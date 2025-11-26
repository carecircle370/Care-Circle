import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CareCircleLoginGUI extends JFrame {

    // username,pin,role,fullName,title
    private static final String USER_FILE = "users.txt";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // Login fields
    private JTextField loginUsernameField;
    private JPasswordField loginPinField;

    // Signup fields
    private JTextField signupUsernameField;
    private JPasswordField signupPinField;
    private JPasswordField signupConfirmPinField;
    private JTextField signupFullNameField;
    private JComboBox<String> signupRoleCombo;
    private JTextField signupDoctorTitleField;
    private JLabel signupDoctorTitleLabel;

    // Colors (simple health-app palette)
    private static final Color PRIMARY_BLUE  = new Color(37, 150, 190);
    private static final Color HEALTH_GREEN  = new Color(76, 175, 80);
    private static final Color LIGHT_BG      = new Color(245, 250, 255);

    public CareCircleLoginGUI() {
        setTitle("Care Circle – Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        root.setLayout(cards);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        root.setBackground(LIGHT_BG);

        root.add(buildLoginPanel(), "login");
        root.add(buildSignupPanel(), "signup");

        setContentPane(root);

        ensureUserFileExists();
        cards.show(root, "login");
    }

    // Make sure users.txt exists; if not, create with a few demo accounts (5 fields)
    private void ensureUserFileExists() {
        try {
            Path p = Path.of(USER_FILE);
            if (!Files.exists(p)) {
                try (BufferedWriter bw = Files.newBufferedWriter(p)) {
                    bw.write("doctor,1234,doctor,Dr. Demo,Cardiologist\n");
                    bw.write("patient,1234,patient,Patient Demo,\n");
                    bw.write("caregiver,1234,caregiver,Caregiver Demo,\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Small helper for GridBag
    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        return c;
    }

    // ========== LOGIN PANEL ==========

    // ========== LOGIN PANEL ==========

    private JPanel buildLoginPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(LIGHT_BG);

        // ---- Top header with logo-style text ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 252, 248)); // light mint like your logo bg
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Logo text (green) + small orange dot like example
        JLabel logoLabel = new JLabel(
                "<html><span style='color:#00745C;font-weight:bold;'>Care Circle</span></html>"
        );
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Your health. Your circle. Always together.");
        subtitle.setForeground(new Color(90, 90, 90));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.add(logoLabel);
        textBox.add(Box.createVerticalStrut(4));
        textBox.add(subtitle);

        // Blue accent bar on the right
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(10, 0));
        accent.setBackground(PRIMARY_BLUE);

        header.add(textBox, BorderLayout.WEST);
        header.add(accent, BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);

        // ---- Center card with form ----
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 235, 240)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Sign in to your account");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        loginUsernameField = new JTextField(15);
        loginPinField = new JPasswordField(4);

        JButton loginButton = new JButton("Login");
        loginButton.setBackground(HEALTH_GREEN);
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);

        JButton goSignup = new JButton("Create new account");
        goSignup.setBorderPainted(false);
        goSignup.setContentAreaFilled(false);
        goSignup.setForeground(PRIMARY_BLUE);
        goSignup.setFocusPainted(false);

        // row 0: title
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        form.add(title, c);

        // row 1: username
        c.gridwidth = 1;
        c.gridy = 1; c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel("Username"), c);
        c.gridx = 1;
        form.add(loginUsernameField, c);

        // row 2: PIN
        c.gridy = 2; c.gridx = 0;
        form.add(new JLabel("PIN (4 digits)"), c);
        c.gridx = 1;
        form.add(loginPinField, c);

        // row 3: login button
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        form.add(loginButton, c);

        // row 4: signup link
        c.gridy = 4;
        form.add(goSignup, c);

        card.add(form, new GridBagConstraints());
        main.add(card, BorderLayout.CENTER);

        // actions stay the same
        loginButton.addActionListener(this::handleLogin);
        loginPinField.addActionListener(this::handleLogin);
        goSignup.addActionListener(e -> cards.show(root, "signup"));

        return main;
    }


    // ========== SIGNUP PANEL ==========

    private JPanel buildSignupPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(LIGHT_BG);

        // header bar (same style as login)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Create Care Circle Account");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        header.add(title, BorderLayout.WEST);
        main.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(LIGHT_BG);
        GridBagConstraints c = gbc();

        signupUsernameField = new JTextField(15);
        signupFullNameField = new JTextField(15);
        signupPinField = new JPasswordField(4);
        signupConfirmPinField = new JPasswordField(4);

        signupRoleCombo = new JComboBox<>(new String[]{
                "Patient", "Doctor", "Caregiver/Family"
        });

        signupDoctorTitleField = new JTextField(15);
        signupDoctorTitleLabel = new JLabel("Doctor Title");

        JButton createButton = new JButton("Create Account");
        createButton.setBackground(HEALTH_GREEN);
        createButton.setForeground(Color.BLACK);
        createButton.setFocusPainted(false);

        JButton backButton = new JButton("Back to Login");
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setForeground(PRIMARY_BLUE);
        backButton.setFocusPainted(false);

        // Initially hide doctor title field (only for doctors)
        setDoctorTitleVisible(false);

        // Username
        c.gridx = 0; c.gridy = 0; c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel("Username"), c);
        c.gridx = 1;
        form.add(signupUsernameField, c);

        // Full name
        c.gridy = 1; c.gridx = 0;
        form.add(new JLabel("Full name"), c);
        c.gridx = 1;
        form.add(signupFullNameField, c);

        // Role
        c.gridy = 2; c.gridx = 0;
        form.add(new JLabel("Role"), c);
        c.gridx = 1;
        form.add(signupRoleCombo, c);

        // Doctor title (shown only when role = Doctor)
        c.gridy = 3; c.gridx = 0;
        form.add(signupDoctorTitleLabel, c);
        c.gridx = 1;
        form.add(signupDoctorTitleField, c);

        // PIN
        c.gridy = 4; c.gridx = 0;
        form.add(new JLabel("PIN (4 digits)"), c);
        c.gridx = 1;
        form.add(signupPinField, c);

        // Confirm PIN
        c.gridy = 5; c.gridx = 0;
        form.add(new JLabel("Confirm PIN"), c);
        c.gridx = 1;
        form.add(signupConfirmPinField, c);

        // Create button
        c.gridy = 6; c.gridx = 0; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        form.add(createButton, c);

        // Back button
        c.gridy = 7;
        form.add(backButton, c);

        main.add(form, BorderLayout.CENTER);

        // Actions
        signupRoleCombo.addActionListener(e -> {
            String selected = (String) signupRoleCombo.getSelectedItem();
            boolean isDoctor = "Doctor".equals(selected);
            setDoctorTitleVisible(isDoctor);
            // optional: clear title when not doctor
            if (!isDoctor) {
                signupDoctorTitleField.setText("");
            }
            main.revalidate();
            main.repaint();
        });

        createButton.addActionListener(this::handleSignup);
        backButton.addActionListener(e -> cards.show(root, "login"));

        return main;
    }

    private void setDoctorTitleVisible(boolean visible) {
        if (signupDoctorTitleLabel != null) {
            signupDoctorTitleLabel.setVisible(visible);
        }
        if (signupDoctorTitleField != null) {
            signupDoctorTitleField.setVisible(visible);
            signupDoctorTitleField.setEnabled(visible);
        }
    }

    // ========== ACTION HANDLERS ==========

    private void handleLogin(ActionEvent e) {
        String username = loginUsernameField.getText().trim().toLowerCase(Locale.ROOT);
        String pin = new String(loginPinField.getPassword()).trim();

        if (username.isEmpty() || pin.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and PIN.",
                    "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        CareCircleUser user = findUser(username, pin);
        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or PIN.",
                    "Login failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        openHomeFor(user);
    }

    private void handleSignup(ActionEvent e) {
        String username = signupUsernameField.getText().trim().toLowerCase(Locale.ROOT);
        String fullName = signupFullNameField.getText().trim();
        String pin = new String(signupPinField.getPassword()).trim();
        String confirmPin = new String(signupConfirmPinField.getPassword()).trim();
        String roleDisplay = (String) signupRoleCombo.getSelectedItem();

        if (username.isEmpty() || fullName.isEmpty() ||
                pin.isEmpty() || confirmPin.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields.",
                    "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pin.equals(confirmPin)) {
            JOptionPane.showMessageDialog(this,
                    "PIN and Confirm PIN do not match.",
                    "PIN mismatch",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pin.length() != 4 || !pin.chars().allMatch(Character::isDigit)) {
            JOptionPane.showMessageDialog(this,
                    "PIN must be exactly 4 digits.",
                    "Invalid PIN",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isUsernameTaken(username)) {
            JOptionPane.showMessageDialog(this,
                    "This username is already taken.",
                    "Duplicate username",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String role;
        String doctorTitle = "";
        if ("Doctor".equals(roleDisplay)) {
            role = "doctor";
            doctorTitle = signupDoctorTitleField.getText().trim();
            if (doctorTitle.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter your doctor title (e.g., Cardiologist).",
                        "Missing title",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else if ("Caregiver/Family".equals(roleDisplay)) {
            role = "caregiver";
        } else {
            role = "patient";
        }

        // Save user to text file: username,pin,role,fullName,title
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            bw.write(username + "," + pin + "," + role + "," + fullName + "," + doctorTitle);
            bw.newLine();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save user file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Account created! You can now log in.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

        // Clear signup fields & go back to login
        signupUsernameField.setText("");
        signupFullNameField.setText("");
        signupPinField.setText("");
        signupConfirmPinField.setText("");
        signupDoctorTitleField.setText("");
        signupRoleCombo.setSelectedIndex(0);
        setDoctorTitleVisible(false);

        loginUsernameField.setText(username);
        loginPinField.setText("");
        cards.show(root, "login");
    }

    private boolean isUsernameTaken(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 1) {
                    String existing = parts[0].trim().toLowerCase(Locale.ROOT);
                    if (existing.equals(username)) {
                        return true;
                    }
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    private CareCircleUser findUser(String username, String pin) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 4) {
                    String u = parts[0].trim().toLowerCase(Locale.ROOT);
                    String p = parts[1].trim();
                    String role = parts[2].trim().toLowerCase(Locale.ROOT);
                    String fullName = parts[3].trim();
                    String title = "";
                    if (parts.length >= 5) {
                        title = parts[4].trim();
                    }
                    if (u.equals(username) && p.equals(pin)) {
                        return new CareCircleUser(u, fullName, role, title);
                    }
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    private void openHomeFor(CareCircleUser user) {
        String role = user.getRole().toLowerCase(Locale.ROOT);

        JFrame home;
        if (role.startsWith("doctor")) {
            home = new DoctorHomeUI(user);
        } else if (role.startsWith("caregiver") || role.startsWith("family")) {
            home = new CaregiverFamilyHomeUI(user);
        } else {
            home = new PatientHomeUI(user);
        }

        home.setVisible(true);
        dispose(); // close login window
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new CareCircleLoginGUI().setVisible(true));
    }
}
