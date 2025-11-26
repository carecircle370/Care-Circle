import javax.swing.*;
import java.awt.*;

public class DoctorHomeUI extends JFrame {

    private final CareCircleUser user;

    public DoctorHomeUI(CareCircleUser user) {
        this.user = user;

        setTitle("Care Circle – Doctor UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));
       JComponent content = (JComponent) getContentPane();
       content.setBackground(new Color(245, 250, 255));
       content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
    }

    // ================================
    // HEADER (Profile Area)
    // ================================
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        Color bg = new Color(232, 248, 244);        // soft green
        header.setBackground(bg);

        // Avatar: teal border for doctor, light grey body
        AvatarPanel avatar = new AvatarPanel(new Color(37, 150, 190),
                new Color(230, 230, 230));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        info.add(new JLabel("Username: " + user.getUsername()));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Doctor Full Name: " + user.getFullName()));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Doctor Title: " + user.getTitle()));
        info.add(Box.createVerticalStrut(4));

        JButton editProfile = new JButton("Edit Profile");
        info.add(editProfile);

        header.add(avatar, BorderLayout.WEST);
        header.add(info, BorderLayout.CENTER);

        return header;
    }


    // ================================
    // MAIN AREA (Patients List + Chat)
    // ================================
    private JComponent buildMainArea() {
        JPanel main = new JPanel(new GridLayout(1, 2, 10, 0));
        main.add(buildPatientsPanel());
        main.add(buildChatPanel());
        return main;
    }

    // ================================
    // LEFT PANEL: List of Patients
    // ================================
    private JComponent buildPatientsPanel() {
        DefaultListModel<String> model = new DefaultListModel<>();

        // Demo patient list — YOU can load real patients later
        model.addElement(" URGENT Mark Smith – Cardiac Arrest");
        model.addElement("John Doe – Follow-up Consultation");
        model.addElement("Maria Lopez – Medication Review");

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Red highlight for urgent cases
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                String text = value.toString();
                if (text.contains("URGENT")) {
                    lbl.setForeground(Color.RED);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                }
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder("List of Patients & Tasks "));

        // Button for further patient actions
        JButton btnViewPatient = new JButton("View Selected Patient Info");
        btnViewPatient.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a patient.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Doctor can view patient details:\n" + selected +
                                "\n(You can link this to vitals/appointments txt files.)");
            }
        });

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(btnViewPatient);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ================================
    // RIGHT PANEL: Chat Window
    // ================================
    private JComponent buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Chat Box"));

        JTextArea info = new JTextArea(
                "This is a placeholder chat panel.\n" +
                        "Click the button below to open the real chat window."
        );
        info.setEditable(false);
        info.setLineWrap(true);

        JButton btnOpenChat = new JButton("Open Chat Window");
        btnOpenChat.addActionListener(e -> {
            // ✔ Uses YOUR Chat System Client.java
            new Chat(user.getUsername(), "localhost", 1234);
        });

        panel.add(new JScrollPane(info), BorderLayout.CENTER);
        panel.add(btnOpenChat, BorderLayout.SOUTH);

        return panel;
    }
}
