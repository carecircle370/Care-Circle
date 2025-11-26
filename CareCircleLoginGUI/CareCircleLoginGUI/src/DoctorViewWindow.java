import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DoctorViewWindow extends JFrame {
    private final String host;
    private final int port;

    private final JTextField tfPatientId = new JTextField(14);
    private final JButton btnLoadPatient = new JButton("Load Patient");
    private final JButton btnLoadAll     = new JButton("Load All");

    private final String[] COLS = {
            "patientId","heartRateBpm","bpSystolic","bpDiastolic",
            "temperatureC","mood","dietNotes","weightKg","submittedAt"
    };
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);


    public DoctorViewWindow(Frame owner, String host, int port) {
        super("Doctor View — Vitals");
        this.host = host;
        this.port = port;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildUI());
        setSize(900, 500);
        setLocationRelativeTo(owner);
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(12,12,12,12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Patient ID:"));
        top.add(tfPatientId);
        top.add(btnLoadPatient);
        top.add(btnLoadAll);

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        btnLoadPatient.addActionListener(e -> load(false));
        btnLoadAll.addActionListener(e -> load(true));

        return root;
    }

    private void load(boolean all) {
        String cmd;
        if (all) {
            cmd = "LIST ALL";
        } else {
            String pid = tfPatientId.getText().trim();
            if (pid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a patient ID, or click 'Load All'.",
                        "Missing Patient ID", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            cmd = "LIST " + pid;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnLoadAll.setEnabled(false);
        btnLoadPatient.setEnabled(false);
        model.setRowCount(0);

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override protected List<String> doInBackground() {
                return sendList(cmd);
            }
            @Override protected void done() {
                try {
                    List<String> lines = get();
                    if (lines == null || lines.isEmpty()) {
                        JOptionPane.showMessageDialog(DoctorViewWindow.this, "No data returned.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int start = 0;
                    if (lines.get(0).toLowerCase().startsWith("patientid,")) start = 1;

                    for (int i = start; i < lines.size(); i++) {
                        String ln = lines.get(i);
                        if (ln == null || ln.trim().isEmpty() || "END".equals(ln)) continue;
                        List<String> fields = parseCsvLine(ln);
                        Object[] row = new Object[COLS.length];
                        for (int c = 0; c < COLS.length; c++) row[c] = (c < fields.size()) ? fields.get(c) : "";
                        model.addRow(row);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DoctorViewWindow.this, "Load failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    btnLoadAll.setEnabled(true);
                    btnLoadPatient.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /** Send LIST and read lines until END or error (with timeout). */
    private List<String> sendList(String command) {
        List<String> out = new ArrayList<>();
        try (Socket socket = new Socket(host, port);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(3000); // prevent hangs
            bw.write(command); bw.newLine(); bw.flush();

            String ln;
            while ((ln = br.readLine()) != null) {
                out.add(ln);
                if ("END".equals(ln)) break;
                if (ln.startsWith("ERROR:")) break;
            }
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
        }
        return out;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else { inQuotes = false; }
                } else cur.append(ch);
            } else {
                if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (ch == '"') { inQuotes = true; }
                else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DoctorViewWindow w = new DoctorViewWindow(null, "localhost", 1234);
            w.setVisible(true);
        });
    }
}