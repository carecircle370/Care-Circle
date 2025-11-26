// ============================================================================
// File: src/com/carecircle/ui/VitalsSubmitPanel.java
// ============================================================================
package com.carecircle.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Patient-side vitals submission over TCP to VitalsTcpServer. */
public final class VitalsSubmitPanel extends JPanel {
    private static final String HOST = "localhost";
    private static final int PORT = 1234;

    private final JTextField tfPatientId   = new JTextField(16);
    private final JTextField tfPatientName = new JTextField(16);
    private final JFormattedTextField tfHeartRate = numericField(false);
    private final JFormattedTextField tfBpSys     = numericField(false);
    private final JFormattedTextField tfBpDia     = numericField(false);
    private final JFormattedTextField tfTempC     = numericField(true);
    private final JComboBox<String> cbMood = new JComboBox<>(new String[]{"","VERY_BAD","BAD","NEUTRAL","GOOD","VERY_GOOD"});
    private final JTextArea  taDietNotes   = new JTextArea(3, 16);
    private final JFormattedTextField tfWeightKg  = numericField(true);
    private final JTextArea  taLog         = new JTextArea(6, 40);

    private final Border defaultBorder = tfPatientId.getBorder();
    private final String sessionPatientId;
    private final String sessionPatientName;
    private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public VitalsSubmitPanel() {
        this(null, "");
    }

    public VitalsSubmitPanel(String patientId, String patientName) {
        this.sessionPatientId = patientId;
        this.sessionPatientName = patientName == null ? "" : patientName;
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(12,12,12,12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL; c.weightx=1.0;
        int r=0;
        r = addRowWithHint(form,c,r,"Patient ID", tfPatientId, "Example: P12345");
        r = addRowWithHint(form,c,r,"Patient Name", tfPatientName, "Example: Avery Lee");
        r = addRowWithHint(form,c,r,"Heart Rate (bpm)*", tfHeartRate, "Digits only, e.g., 72");
        r = addRowWithHint(form,c,r,"BP Sys*", tfBpSys, "Digits only, e.g., 120");
        r = addRowWithHint(form,c,r,"BP Dia*", tfBpDia, "Digits only, e.g., 80");
        r = addRowWithHint(form,c,r,"Temp (°C)*", tfTempC, "Digits only, e.g., 36.7");
        r = addRowWithHint(form,c,r,"Mood", cbMood, "Choose from the list; leave blank if unsure.");
        taDietNotes.setLineWrap(true); taDietNotes.setWrapStyleWord(true);
        r = addRowWithHint(form,c,r,"Diet Notes", new JScrollPane(taDietNotes), "Short notes, e.g., 'Oatmeal for breakfast'.");
        r = addRowWithHint(form,c,r,"Weight (kg)*", tfWeightKg, "Digits only, e.g., 68.5");

        JLabel hint = hintLabel("Fields marked with * are required. Numbers should be typed as digits (no symbols).");
        c.gridx = 0; c.gridy = r; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(hint, c);
        c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        JButton btnSubmit = new JButton("Submit");
        JButton btnClear  = new JButton("Clear");
        JButton btnQuit   = new JButton("Quit Session");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnSubmit); buttons.add(btnClear); buttons.add(btnQuit);

        taLog.setEditable(false);
        taLog.setBorder(new EmptyBorder(6,6,6,6));
        JScrollPane log = new JScrollPane(taLog);
        log.setBorder(new TitledBorder("Session Log"));

        JPanel header = new JPanel(new BorderLayout());
        if (sessionPatientId != null) {
            tfPatientId.setText(sessionPatientId);
            tfPatientId.setEditable(false);
            tfPatientName.setText(sessionPatientName);
            JLabel banner = new JLabel("You are submitting as " + bannerText());
            banner.setFont(banner.getFont().deriveFont(Font.BOLD));
            banner.setBorder(new EmptyBorder(0,0,8,0));
            header.add(banner, BorderLayout.NORTH);
        }
        header.add(form, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(log, BorderLayout.SOUTH);

        btnSubmit.addActionListener(e -> onSubmit());
        btnClear.addActionListener(e -> clearForm());
        btnQuit.addActionListener(e -> onQuit());
    }

    private int addRowWithHint(JPanel p, GridBagConstraints c, int row, String label, Component field, String hintText)
    {
        c.gridx=0; c.gridy=row; c.weightx=0; c.gridwidth=1; p.add(new JLabel(label), c);
        c.gridx=1; c.weightx=1; c.gridwidth=1; p.add(field, c);

        c.gridx=0; c.gridy=row+1; c.weightx=1; c.gridwidth=2;
        p.add(hintLabel(hintText), c);
        c.gridwidth=1;
        return row+2;
    }

    private JLabel hintLabel(String text){
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(new Color(20, 40, 80));
        lbl.setBorder(new EmptyBorder(0,16,0,0));
        return lbl;
    }

    private void onSubmit() {
        resetValidation();

        if (markIfBlank(tfPatientId)) { JOptionPane.showMessageDialog(this, "Please enter the patient ID before submitting."); return; }
        if (sessionPatientId == null && markIfBlank(tfPatientId)) { JOptionPane.showMessageDialog(this, "Patient ID required."); return; }
        boolean missing = markIfBlank(tfHeartRate) | markIfBlank(tfBpSys) | markIfBlank(tfBpDia)
                | markIfBlank(tfTempC) | markIfBlank(tfWeightKg);
        if (missing) { JOptionPane.showMessageDialog(this, "Fill in the required vitals highlighted in red."); return; }

        if (!validateNumeric(tfHeartRate, "Heart rate") || !validateNumeric(tfBpSys, "Systolic BP")
                || !validateNumeric(tfBpDia, "Diastolic BP") || !validateNumeric(tfTempC, "Temperature")
                || !validateNumeric(tfWeightKg, "Weight")) {
            return;
        }
        String patientId = sessionPatientId != null ? sessionPatientId : txt(tfPatientId);
        String line = csvJoin(
                patientId,
                txt(tfPatientName),
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

        if (resp != null && resp.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, "Vitals not accepted: " + resp, "Submission problem", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String summary = "Vitals submitted for patient " + txt(tfPatientId) + "\n"
                + "Heart rate: " + txt(tfHeartRate) + " bpm\n"
                + "Blood pressure: " + txt(tfBpSys) + "/" + txt(tfBpDia) + " mmHg\n"
                + "Temperature: " + txt(tfTempC) + " °C\n"
                + "Weight: " + txt(tfWeightKg) + " kg\n"
                + "Mood: " + val(cbMood) + (txt(taDietNotes).isEmpty()?"":"\nDiet notes: " + txt(taDietNotes));
        JOptionPane.showMessageDialog(this, summary, "Vitals submitted", JOptionPane.INFORMATION_MESSAGE);
        if (!resp.startsWith("ERROR")) {
            showConfirmation(patientId);
        }
    }
    private void onQuit() {
        appendLog("> QUIT");
        String resp = sendSingle("QUIT");
        appendLog("< " + resp);
    }

    private String sendSingle(String line) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)))
        {
            bw.write(line); bw.newLine(); bw.flush();
            socket.setSoTimeout(1500);
            String first = br.readLine();
            if (first == null) return "<no response>";
            String second = br.ready() ? br.readLine() : null;
            return second != null ? first + " | " + second : first;
        } catch (IOException ex)
        {
            return "ERROR: " + ex.getMessage();
        }
    }

    private static String txt(JTextField tf) { String s = tf.getText(); return s==null?"":s.trim(); }
    private static String txt(JTextArea ta) { String s = ta.getText(); return s==null?"":s.trim(); }
    private static String val(JComboBox<String> cb) { Object v=cb.getSelectedItem(); return v==null?"":v.toString(); }
    private static String escapeCsv(String s){
        if (s==null) return "";
        boolean q=s.contains(",")||s.contains("\"")||s.contains("\n")|| s.contains("\r");
        String t=s.replace("\"","\"\"");
        return q?("\""+t+"\""):t;
    }
    private static String csvJoin(String... fields){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<fields.length;i++){ sb.append(escapeCsv(fields[i]));
            if(i<fields.length-1) sb.append(',');
        }
        return sb.toString();
    }
    private void clearForm(){
        if (sessionPatientName != null && !sessionPatientName.isBlank()) {
            tfPatientName.setText(sessionPatientName);
        } else {
            tfPatientName.setText("");
        }
        tfHeartRate.setText("");
        tfBpSys.setText("");
        tfBpDia.setText("");
        tfTempC.setText("");
        cbMood.setSelectedIndex(0);
        taDietNotes.setText("");
        tfWeightKg.setText("");
        resetValidation();
    }
    private void appendLog(String s)
    {
        taLog.append("["+ LOG_TS.format(Instant.now()) +"] "+s+"\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }

    private void showConfirmation(String patientId) {
        String timestamp = LOG_TS.format(Instant.now());
        StringBuilder sb = new StringBuilder("<html><h3>Vitals submitted</h3><ul>");
        sb.append("<li>For: ").append(bannerText(patientId)).append("</li>");
        sb.append("<li>At: ").append(timestamp).append("</li>");
        sb.append("<li>Heart Rate: ").append(txt(tfHeartRate)).append(" bpm</li>");
        sb.append("<li>Blood Pressure: ").append(txt(tfBpSys)).append(" / ").append(txt(tfBpDia)).append(" mmHg</li>");
        sb.append("<li>Temperature: ").append(txt(tfTempC)).append(" °C</li>");
        sb.append("<li>Weight: ").append(txt(tfWeightKg)).append(" kg</li>");
        if (!val(cbMood).isBlank()) sb.append("<li>Mood: ").append(val(cbMood)).append("</li>");
        if (!txt(taDietNotes).isBlank()) sb.append("<li>Diet Notes: ").append(escapeCsv(txt(taDietNotes))).append("</li>");
        sb.append("</ul></html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "Vitals Submitted", JOptionPane.INFORMATION_MESSAGE);
    }

    private String bannerText() {
        return bannerText(sessionPatientId);
    }
    private String bannerText(String pid) {
        String name = sessionPatientName == null ? "" : sessionPatientName;
        if (name.isBlank()) return "Patient ID " + pid;
        return name + " (" + pid + ")";
    }

    private static JFormattedTextField numericField(boolean allowDecimal){
        NumberFormatter nf = new NumberFormatter();
        nf.setValueClass(allowDecimal ? Double.class : Integer.class);
        nf.setAllowsInvalid(true);   // allow users to type freely, validation runs on submit
        nf.setMinimum(null);         // allow clearing the field

        JFormattedTextField tf = new JFormattedTextField(nf);
        tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        tf.setColumns(12);
        return tf;
    }

    private void resetValidation(){
        tfPatientId.setBorder(defaultBorder);
        tfHeartRate.setBorder(defaultBorder);
        tfBpSys.setBorder(defaultBorder);
        tfBpDia.setBorder(defaultBorder);
        tfTempC.setBorder(defaultBorder);
        tfWeightKg.setBorder(defaultBorder);
    }
    private boolean markIfBlank(JTextField tf){
        if (tf.getText().trim().isEmpty()) { markInvalid(tf); return true; }
        return false;
    }
    private boolean validateNumeric(JTextField tf, String label){
        try {
            Double.parseDouble(tf.getText().trim());
            return true;
        } catch (NumberFormatException ex){
            markInvalid(tf);
            JOptionPane.showMessageDialog(this, label + " must be numbers only. Please type digits.");
            return false;
        }
    }
    private void markInvalid(JTextField tf){ tf.setBorder(BorderFactory.createLineBorder(Color.RED)); }


}