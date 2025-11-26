package com.carecircle.ui;

import com.carecircle.core.VitalsDispatch;
import com.carecircle.core.VitalsRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class VitalsViewerPanel extends JPanel {
    private final VitalsDispatch vitals;

    private final JTextField tfPatientId = new JTextField(12);
    private final JButton btnLoad = new JButton("Load");
    private final JButton btnLoadAll = new JButton("Load All");
    private final JComboBox<String> cbMonth = new JComboBox<>(monthNames());
    private final JSpinner spYear = new JSpinner(new SpinnerNumberModel(Year.now().getValue(), 2000, 2100, 1));
    private final JButton btnMonthlyAvg = new JButton("Monthly Ave");
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblScopeHint = new JLabel(" ");
    private final JLabel lblInfo = new JLabel(" "); // helpful hint / banner

    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Patient ID","Patient Name","HR","Sys","Dia","Temp °C","Mood","Weight kg","Submitted"
    },0){ @Override public boolean isCellEditable(int r,int c){ return false; } };

    private final JTable table = new JTable(model);
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel center = new JPanel(centerLayout);

    private final String sessionPatientId;
    private final String sessionPatientName;
    private final String scopeHint;
    private final boolean allowPatientIdTyping;

    public VitalsViewerPanel(VitalsDispatch vitals) {
        this(vitals, null, "", null);
    }

    public VitalsViewerPanel(VitalsDispatch vitals, String patientId, String patientName) {
        this(vitals, patientId, patientName, null);
    }

    public VitalsViewerPanel(VitalsDispatch vitals, String patientId, String patientName, String scopeHint) {
        this(vitals, patientId, patientName, scopeHint, true);
    }

    public VitalsViewerPanel(VitalsDispatch vitals, String patientId, String patientName, String scopeHint, boolean allowTyping) {
        this.vitals = Objects.requireNonNull(vitals);
        // Normalise sessionPatientId so blank strings behave like null (unlocked)
        this.sessionPatientId = (patientId == null || patientId.isBlank()) ? null : patientId;
        this.sessionPatientName = patientName == null ? "" : patientName;
        this.scopeHint = scopeHint == null ? "" : scopeHint;
        this.allowPatientIdTyping = allowTyping;

        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(12,12,12,12));

        JLabel steps = new JLabel("Step 1: Enter a patient or browse all. Step 2: Load vitals or monthly averages.");
        steps.setFont(steps.getFont().deriveFont(Font.BOLD, 13f));
        steps.setBorder(new EmptyBorder(0, 0, 6, 0));

        Dimension primarySize = new Dimension(130, 32);
        Font primaryFont = btnLoad.getFont().deriveFont(Font.BOLD, 13f);
        for (JButton b : new JButton[]{btnLoad, btnLoadAll, btnMonthlyAvg}) {
            b.setPreferredSize(primarySize);
            b.setFont(primaryFont);
            b.setMargin(new Insets(8, 14, 8, 14));
        }

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));
        filters.setBorder(new EmptyBorder(8, 10, 8, 10));
        filters.add(new JLabel("Patient ID:"));
        if (!allowPatientIdTyping) {
            tfPatientId.setEditable(false);
            tfPatientId.setBackground(new Color(0xF7F7F7));
            tfPatientId.setToolTipText("Select a row to populate this field or click Load All.");
        }
        filters.add(tfPatientId);
        filters.add(btnLoad);
        filters.add(btnLoadAll);
        filters.add(new JLabel("Month:"));
        filters.add(cbMonth);
        filters.add(new JLabel("Year:"));
        filters.add(spYear);
        filters.add(btnMonthlyAvg);
        if (!this.scopeHint.isBlank()) {
            lblScopeHint.setText(this.scopeHint);
            lblScopeHint.setForeground(Color.DARK_GRAY);
            lblScopeHint.setFont(lblScopeHint.getFont().deriveFont(Font.ITALIC, lblScopeHint.getFont().getSize2D()));
            lblScopeHint.setToolTipText(this.scopeHint);
            filters.add(lblScopeHint);
        }

        // Top composite: steps, filters, and an info banner below filters
        JPanel top = new JPanel(new BorderLayout());
        top.add(steps, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        lblInfo.setForeground(Color.DARK_GRAY);
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.ITALIC, 12f));
        lblInfo.setBorder(new EmptyBorder(6, 10, 6, 0));
        top.add(lblInfo, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        if (sessionPatientId != null) {
            tfPatientId.setText(sessionPatientId);
            tfPatientId.setEditable(false);
            btnLoadAll.setVisible(false);
            JLabel banner = new JLabel("You are submitting as " + bannerText());
            banner.setFont(banner.getFont().deriveFont(Font.BOLD));
            banner.setBorder(new EmptyBorder(0,0,6,0));
            header.add(banner, BorderLayout.NORTH);
        }
        header.add(top, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!allowPatientIdTyping && !table.getSelectionModel().getValueIsAdjusting()) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    Object pid = model.getValueAt(modelRow, 0);
                    tfPatientId.setText(pid == null ? "" : pid.toString());
                    applyRowFilter();
                    updateSummary();
                }
            }
        });
        center.add(new JScrollPane(table), "table");
        JLabel empty = new JLabel("No vitals loaded.", SwingConstants.CENTER);
        empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 14f));
        center.add(empty, "empty");
        add(center, BorderLayout.CENTER);
        lblSummary.setBorder(new EmptyBorder(4,0,0,0));
        add(lblSummary, BorderLayout.SOUTH);

        tfPatientId.setText(tfPatientId.getText().trim());

        btnLoad.addActionListener(e -> loadByPatient());
        btnLoadAll.addActionListener(e -> render(vitals.listAll()));
        btnMonthlyAvg.addActionListener(e -> showMonthlyAvg());
        tfPatientId.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyRowFilter(); updateSummary(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyRowFilter(); updateSummary(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyRowFilter(); updateSummary(); }
        });

        if (sessionPatientId != null) { render(vitals.listByPatient(sessionPatientId)); }
        else { render(vitals.listAll()); }
    }

    private void loadByPatient(){
        String pid = resolvePatientId();
        if (pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter Patient ID."); return; }
        render(vitals.listByPatient(pid));
    }

    private void showMonthlyAvg(){
        String pid = resolvePatientId();
        if (pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter Patient ID."); return; }
        int month = cbMonth.getSelectedIndex() + 1;
        int year = (Integer) spYear.getValue();

        List<VitalsRecord> rows = vitals.listByPatient(pid).stream()
                .filter(v -> v.submittedAt()!=null)
                .filter(v -> {
                    var z = v.submittedAt().atZone(ZoneId.systemDefault());
                    return z.getYear()==year && z.getMonthValue()==month;
                })
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data for "+pid+" in "+cbMonth.getSelectedItem()+" "+year);
            return;
        }

        double hr=0,sys=0,dia=0,tc=0,kg=0; int nhr=0,nsys=0,ndia=0,ntc=0,nkg=0;
        for (VitalsRecord v : rows) {
            if (v.heartRateBpm()!=null){ hr+=v.heartRateBpm(); nhr++; }
            if (v.bpSystolic()!=null){ sys+=v.bpSystolic(); nsys++; }
            if (v.bpDiastolic()!=null){ dia+=v.bpDiastolic(); ndia++; }
            if (v.temperatureC()!=null){ tc+=v.temperatureC(); ntc++; }
            if (v.weightKg()!=null){ kg+=v.weightKg(); nkg++; }
        }

        String msg = "<html><h3>Monthly Average for " + pid + " (" + cbMonth.getSelectedItem() + " " + year + ")</h3>"
                + "<ul>"
                + "<li>Heart Rate: " + avg(hr,nhr) + " bpm</li>"
                + "<li>BP Systolic: " + avg(sys,nsys) + " mmHg</li>"
                + "<li>BP Diastolic: " + avg(dia,ndia) + " mmHg</li>"
                + "<li>Temp: " + avg(tc,ntc) + " °C</li>"
                + "<li>Weight: " + avg(kg,nkg) + " kg</li>"
                + "</ul>"
                + "<div style='margin-top:6px'>Records: " + rows.size() + "</div></html>";
        JOptionPane.showMessageDialog(this, msg, "Monthly Ave", JOptionPane.INFORMATION_MESSAGE);
    }

    private void render(List<VitalsRecord> list){
        model.setRowCount(0);
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for (VitalsRecord v : list){
            model.addRow(new Object[]{
                    nz(v.patientId()),
                    nz(v.patientName()),
                    v.heartRateBpm()==null?"":v.heartRateBpm(),
                    v.bpSystolic()==null?"":v.bpSystolic(),
                    v.bpDiastolic()==null?"":v.bpDiastolic(),
                    v.temperatureC()==null?"":v.temperatureC(),
                    nz(v.mood()),
                    v.weightKg()==null?"":v.weightKg(),
                    v.submittedAt()==null?"":fmt.format(v.submittedAt())
            });
        }
        applyRowFilter();
        updateSummary();
    }

    private String resolvePatientId() {
        if (sessionPatientId != null) return sessionPatientId;
        if (allowPatientIdTyping) return tfPatientId.getText().trim();

        int viewRow = table.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Object pid = model.getValueAt(modelRow, 0);
            if (pid != null) return pid.toString();
        }
        return tfPatientId.getText().trim();
    }

    private void updateSummary(){
        if (model.getRowCount()==0) {
            centerLayout.show(center, "empty");
            lblSummary.setText(" ");
            // Provide helpful hint/banner for empty results
            String pidFilter = tfPatientId.getText().trim();
            if (!pidFilter.isEmpty()) {
                String hint = "No vitals found for \"" + pidFilter + "\".";
                if (!scopeHint.isBlank()) hint += " " + scopeHint;
                hint += " If you are a provider, ensure this patient is assigned to you (Manage Patients). Click Load to try again.";
                lblInfo.setText(hint);
            } else {
                lblInfo.setText("No vitals loaded. Use \"Load All\" to fetch all (subject to your access), or enter a Patient ID and click Load.");
            }
            return;
        }
        centerLayout.show(center, "table");
        int visible = table.getRowSorter()==null ? model.getRowCount() : table.getRowSorter().getViewRowCount();
        lblSummary.setText("Rows: " + visible + " / " + model.getRowCount() + " | Patient filter: "
                + (tfPatientId.getText().trim().isEmpty()?"(none)":tfPatientId.getText().trim()));
        lblInfo.setText(" "); // clear info when there are results
    }

    private void applyRowFilter(){
        if (table.getRowSorter() instanceof javax.swing.table.TableRowSorter<?> sorter){
            String pidFilter = tfPatientId.getText().trim();
            if (pidFilter.isEmpty()) ((javax.swing.table.TableRowSorter<?>) sorter).setRowFilter(null);
            else ((javax.swing.table.TableRowSorter<?>) sorter).setRowFilter(javax.swing.RowFilter.regexFilter("(?i)"+pidFilter,0,1));
        }
    }

    private String bannerText() {
        if (sessionPatientId == null) return "";
        if (sessionPatientName == null || sessionPatientName.isBlank()) return "Patient ID " + sessionPatientId;
        return sessionPatientName + " (" + sessionPatientId + ")";
    }

    private static String[] monthNames(){
        String[] m = new String[12];
        for (int i=0;i<12;i++) {
            Month mo = Month.of(i+1);
            String s = mo.name().toLowerCase();
            m[i] = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return m;
    }
    private static String avg(double sum,int n){ return n==0?"—":String.format("%.2f", sum/n); }
    private static String nz(String s){ return s==null?"":s; }
}