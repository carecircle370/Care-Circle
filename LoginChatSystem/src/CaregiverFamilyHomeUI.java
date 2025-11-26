import javax.swing.*;
import java.awt.*;

public class CaregiverFamilyHomeUI extends JFrame {

    private final CareCircleUser user;

    public CaregiverFamilyHomeUI(CareCircleUser user) {
        this.user = user;

        setTitle("Care Circle – Caregiver/Family UI");
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

    // ============================================================
    // HEADER (Profile section)
    // ============================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        Color bg = new Color(255, 246, 234);        // soft orange/cream
        header.setBackground(bg);

        // Avatar: orange border for caregiver/family
        AvatarPanel avatar = new AvatarPanel(new Color(242, 155, 68),
                new Color(230, 230, 230));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        info.add(new JLabel("Username: " + user.getUsername()));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Role: Caregiver/Family"));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Full Name: " + user.getFullName()));
        info.add(Box.createVerticalStrut(4));

        JButton editProfile = new JButton("Edit Profile");
        info.add(editProfile);

        header.add(avatar, BorderLayout.WEST);
        header.add(info, BorderLayout.CENTER);

        return header;
    }


    // ============================================================
    // MAIN AREA (Patient List + Chat Box)
    // ============================================================

    private JComponent buildMainArea() {
        JPanel main = new JPanel(new GridLayout(1, 2, 10, 0));
        main.add(buildPatientsPanel());
        main.add(buildChatPanel());
        return main;
    }

    // ============================================================
    // LEFT PANEL — List of Patients
    // ============================================================

    private JComponent buildPatientsPanel() {

        DefaultListModel<String> model = new DefaultListModel<>();

        // Demo data — you can load real patients from txt later
        model.addElement("John Doe – Medication Support");
        model.addElement("Maria Lopez – Daily Vitals");
        model.addElement("Elderly Parent – Check-in Reminders");

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder("List of Patients"));

        JButton button = new JButton("View Selected Patient");
        button.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a patient.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Caregiver can view patient details:\n" + selected +
                                "\n(Later, connect this to vitals/reminders files.)");
            }
        });

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(button);

        JPanel container = new JPanel(new BorderLayout(5, 5));
        container.add(scroll, BorderLayout.CENTER);
        container.add(bottom, BorderLayout.SOUTH);

        return container;
    }

    // ============================================================
    // RIGHT PANEL — Chat Box
    // ============================================================

    private JComponent buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Chat Box"));

        JTextArea info = new JTextArea(
                "This is a placeholder chat panel.\n" +
                        "Click the button below to open the real chat window."
        );
        info.setLineWrap(true);
        info.setEditable(false);

        JButton openChat = new JButton("Open Chat Window");
        openChat.addActionListener(e -> {
            // Your existing chat system
            new Chat(user.getUsername(), "localhost", 1000);
        });

        panel.add(new JScrollPane(info), BorderLayout.CENTER);
        panel.add(openChat, BorderLayout.SOUTH);

        return panel;
    }
}
