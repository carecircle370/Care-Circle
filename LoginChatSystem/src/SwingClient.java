import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class SwingClient extends JFrame {
    private static final String HOST = "localhost";
    private static final int PORT = 1000;

    private final JTextField tfPatientId   = new JTextField(16);
    private final JTextField tfHeartRate   = new JTextField(16);
    private final JTextField tfBpSys       = new JTextField(16);
    private final JTextField tfBpDia       = new JTextField(16);
    private final JTextField tfTempC       = new JTextField(16);
    private final JComboBox<String> cbMood = new JComboBox<>(new String[]{
            "", "VERY_BAD", "BAD", "NEUTRAL", "GOOD", "VERY_GOOD"
    });
    private final JTextArea taDietNotes    = new JTextArea(3, 16);
    private final JTextField tfWeightKg    = new JTextField(16);
    private final JTextArea taLog          = new JTextArea(6, 40);

    public SwingClient() {
        super("Vitals Demo Client (Submit)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(buildUI());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(12,12,12,12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;
        addRow(form, c, row++, "Patient ID*", tfPatientId);
        addRow(form, c, row++, "Heart Rate (bpm)", tfHeartRate);
        addRow(form, c, row++, "BP Systolic", tfBpSys);
        addRow(form, c, row++, "BP Diastolic", tfBpDia);
        addRow(form, c, row++, "Temperature (Â°C)", tfTempC);
        addRow(form, c, row++, "Mood", cbMood);

        taDietNotes.setLineWrap(true);
        taDietNotes.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(taDietNotes);
        addRow(form, c, row++, "Diet Notes", notesScroll);
        addRow(form, c, row++, "Weight (kg)", tfWeightKg);

        JButton btnSubmit = new JButton("Submit Vitals");
        JButton btnClear  = new JButton("Clear Form");
        JButton btnQuit   = new JButton("Quit Session");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnSubmit);
        buttons.add(btnClear);
        buttons.add(btnQuit);

        taLog.setEditable(false);
        JScrollPane logScroll = new JScrollPane(taLog);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.add(buttons, BorderLayout.NORTH);
        center.add(logScroll, BorderLayout.CENTER);

        root.add(form, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        btnSubmit.addActionListener(e -> onSubmit());
        btnClear.addActionListener(e -> clearForm());
        btnQuit.addActionListener(e -> onQuit());

        return root;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, Component field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private void onSubmit() {
        String patientId = tfPatientId.getText().trim();
        if (patientId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Patient ID is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String line = csvJoin(
                txt(tfPatientId),
                txt(tfHeartRate),
                txt(tfBpSys),
                txt(tfBpDia),
                txt(tfTempC),
                val(cbMood),
                txt(taDietNotes),
                txt(tfWeightKg)
        );
        appendLog("> " + line);
        String resp = sendSingle(line);
        appendLog("< " + resp);
    }

    private void onQuit() {
        appendLog("> QUIT");
        String resp = sendSingle("QUIT");
        appendLog("< " + resp);
    }

    /** One-line request/response (for submit/quit). */
    private String sendSingle(String line) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            bw.write(line); bw.newLine(); bw.flush();
            socket.setSoTimeout(1500);
            String first = br.readLine();
            if (first == null) return "<no response>";
            String second = br.ready() ? br.readLine() : null;
            return (second != null) ? (first + " | " + second) : first;

        } catch (IOException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }

    // ----- helpers -----
    private static String txt(JTextField tf) { String s = tf.getText(); return s == null ? "" : s.trim(); }
    private static String txt(JTextArea ta)   { String s = ta.getText(); return s == null ? "" : s.trim(); }
    private static String val(JComboBox<String> cb) {
        Object v = cb.getSelectedItem(); return v == null ? "" : v.toString();
    }
    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String t = s.replace("\"", "\"\"");
        return needsQuote ? ("\"" + t + "\"") : t;
    }
    private static String csvJoin(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(escapeCsv(fields[i]));
            if (i < fields.length - 1) sb.append(',');
        }
        return sb.toString(); // server appends submittedAt
    }
    private void clearForm() {
        tfHeartRate.setText(""); tfBpSys.setText(""); tfBpDia.setText("");
        tfTempC.setText(""); cbMood.setSelectedIndex(0);
        taDietNotes.setText(""); tfWeightKg.setText("");
    }
    private void appendLog(String s) {
        taLog.append("[" + Instant.now() + "] " + s + "\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingClient().setVisible(true));
    }
}
