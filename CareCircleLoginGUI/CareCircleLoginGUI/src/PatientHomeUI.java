import javax.swing.*;
import java.awt.*;

public class PatientHomeUI extends JFrame {

    private final CareCircleUser user;

    public PatientHomeUI(CareCircleUser user) {
        this.user = user;

        setTitle("Care Circle – Patient UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));
        JComponent content = (JComponent) getContentPane();
        content.setBackground(new Color(245, 250, 255));
        content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenterArea(), BorderLayout.CENTER);
        add(buildRightButtons(), BorderLayout.EAST);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        Color bg = new Color(240, 248, 255);        // very light blue
        header.setBackground(bg);

        // Avatar: blue border for patient
        AvatarPanel avatar = new AvatarPanel(new Color(90, 155, 215),
                new Color(230, 230, 230));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        info.add(new JLabel("Username: " + user.getUsername()));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Role : Patient "));
        info.add(Box.createVerticalStrut(4));

        info.add(new JLabel("Full Name: " + user.getFullName()));
        info.add(Box.createVerticalStrut(4));

        JButton editProfile = new JButton("Edit Profile");
        info.add(Box.createVerticalStrut(4));
        info.add(editProfile);

        header.add(avatar, BorderLayout.WEST);
        header.add(info, BorderLayout.CENTER);

        return header;
    }


    private JComponent buildCenterArea() {
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.setBackground(new Color(245, 250, 255));
        center.add(buildAppointmentsPanel());
        center.add(buildChatPanel());
        return center;
    }

    private JComponent buildAppointmentsPanel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        // Demo entries – you can later load real appointments from a .txt file.
        model.addElement("Mon 10:00 – Dr. Smith (Telehealth)");
        model.addElement("Wed 09:00 – Blood test reminder");
        model.addElement("Fri 15:30 – Physical therapy");
        model.addElement("Daily 08:00 – Morning medication");

        JList<String> list = new JList<>(model);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Appointments and Reminders (This Week)"
        ));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildChatPanel() {
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);

        JTextField input = new JTextField();
        JButton send = new JButton("Open Chat");

        send.addActionListener(e -> {
            new Chat(user.getUsername(), "localhost", 1234);
        });

        input.addActionListener(e -> send.doClick());

        JPanel bottom = new JPanel(new BorderLayout(5, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Chat Box"));
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildRightButtons() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(220, 0));

        JButton btnVitals = new JButton("View / Enter Daily Vitals");
        JButton btnReminders = new JButton("View All Reminders and Appointments");
        JButton btnProfile = new JButton("Profile");

        Dimension size = new Dimension(200, 80);
        for (JButton b : new JButton[]{btnVitals, btnReminders, btnProfile}) {
            b.setMaximumSize(size);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        // For now just show simple popups – later you can open your real UIs here.
        btnVitals.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Here the patient can view or enter daily vitals.\n" +
                        "(Hook this button to your vitals screen that saves to a .txt file.)"));

        btnReminders.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Here the patient can view all reminders and appointments.\n" +
                        "(Hook this button to your calendar/appointments UI.)"));

        btnProfile.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Here the patient can edit their profile information.\n" +
                        "(You can read/write their data from your user .txt file.)"));

        side.add(btnVitals);
        side.add(Box.createVerticalStrut(10));
        side.add(btnReminders);
        side.add(Box.createVerticalStrut(10));
        side.add(btnProfile);

        return side;
    }
}
