package com.carecircle.ui;

import com.carecircle.core.Dispatchers.CalendarDispatch;
import com.carecircle.data.CalendarDTO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Appointment booking/list/cancel panel using a scoped CalendarDispatch. */
public final class AppointmentsPanel extends JPanel {
    private final CalendarDispatch dispatch;
    private final boolean patientMode;
    private final String sessionPatientId;
    private final String sessionPatientName;

    private final JTextField tfPatientId = new JTextField(16);
    private final JTextField tfPatientName = new JTextField(16);
    private final JTextField tfProfessionalName = new JTextField(16);
    private final JComboBox<String> cbType = new JComboBox<>(new String[]{"Doctor","Nurse","Caregiver","Physiotherapist","Specialist"});
    private final JSpinner spDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JSpinner spTime = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JSpinner spDuration = new JSpinner(new SpinnerNumberModel(30, 15, 240, 15));
    private final JTextField tfReason = new JTextField(16);

    private final JButton btnBook = new JButton("Book");
    private final JButton btnClear = new JButton("Clear");
    private final JButton btnLoadAll = new JButton("Load All");
    private final JButton btnLoadByPatient = new JButton("Load by Patient");
    private final JButton btnCancelSelected = new JButton("Cancel Selected");
    private final JToggleButton btnToggleAll = new JToggleButton("Show All");

    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "ID","Patient ID","Patient Name","Professional","Type","Time","Duration","Reason"
    }, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable table = new JTable(model);

    private final Border defaultFieldBorder = tfPatientId.getBorder();
    private Runnable reloadAction = this::loadAllInternal;

    /** Provider-mode constructor. */
    public AppointmentsPanel(CalendarDispatch dispatch) {
        this.dispatch = Objects.requireNonNull(dispatch);
        this.patientMode = false;
        this.sessionPatientId = null;
        this.sessionPatientName = "";
        buildUi();
        loadAll();
    }

    /** Patient-mode constructor (locks patientId and hides global loaders). */
    public AppointmentsPanel(CalendarDispatch dispatch, String sessionPatientId) {
        this(dispatch, sessionPatientId, "");
    }

    public AppointmentsPanel(CalendarDispatch dispatch, String sessionPatientId, String sessionPatientName) {
        this.dispatch = Objects.requireNonNull(dispatch);
        this.patientMode = true;
        this.sessionPatientId = Objects.requireNonNull(sessionPatientId);
        this.sessionPatientName = sessionPatientName == null ? "" : sessionPatientName;
        buildUi();
        tfPatientId.setText(this.sessionPatientId);
        tfPatientId.setEditable(false);
        if (!this.sessionPatientName.isBlank()) {
            tfPatientName.setText(this.sessionPatientName);
        }
        btnLoadAll.setVisible(false);
        btnLoadByPatient.setVisible(false);
        loadByPatient(this.sessionPatientId);
    }

    private void buildUi() {
        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(16,16,16,16));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets=new Insets(6,6,6,6);
        gbc.anchor=GridBagConstraints.WEST;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        gbc.weightx=1.0;
        int r=0;
        r = addRow(form, gbc, r, "Patient ID:", tfPatientId);
        r = addRow(form, gbc, r, "Patient Name:", tfPatientName);
        r = addRow(form, gbc, r, "Professional Name:", tfProfessionalName);
        r = addRow(form, gbc, r, "Type:", cbType);
        setupDatePicker();
        setupTimePicker();
        r = addRow(form, gbc, r, "Date:", spDate);
        r = addRow(form, gbc, r, "Time:", spTime);
        r = addRow(form, gbc, r, "Duration (min):", spDuration);
        r = addRow(form, gbc, r, "Reason:", tfReason);

        JLabel lblHint = new JLabel("Example: Mar 10, 2025 at 2:30 PM");
        lblHint.setFont(lblHint.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(lblHint, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel steps = new JLabel("Step 1: Enter appointment details. Step 2: Book or load records.");
        steps.setFont(steps.getFont().deriveFont(Font.BOLD, 13f));
        steps.setBorder(new EmptyBorder(0, 4, 6, 4));

        Dimension primarySize = new Dimension(140, 34);
        Font primaryFont = btnBook.getFont().deriveFont(Font.BOLD, 13f);
        for (JButton b : new JButton[]{btnBook, btnLoadAll, btnLoadByPatient}) {
            b.setPreferredSize(primarySize);
            b.setFont(primaryFont);
            b.setMargin(new Insets(8, 16, 8, 16));
        }
        btnCancelSelected.setMargin(new Insets(6, 12, 6, 12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        actions.add(btnBook);
        actions.add(btnClear);
        actions.add(new JSeparator(SwingConstants.VERTICAL));
        actions.add(btnLoadAll);
        actions.add(btnLoadByPatient);
        actions.add(btnToggleAll);
        actions.add(btnCancelSelected);

        JPanel north = new JPanel(new BorderLayout(8,8));
        if (patientMode) {
            JLabel banner = new JLabel("You are submitting as " + bannerText());
            banner.setBorder(new EmptyBorder(0,0,8,0));
            banner.setFont(banner.getFont().deriveFont(Font.BOLD));
            north.add(banner, BorderLayout.NORTH);
        }
        north.add(steps, BorderLayout.WEST);
        north.add(form, BorderLayout.CENTER);
        north.add(actions, BorderLayout.SOUTH);

        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnCancelSelected.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> btnCancelSelected.setEnabled(!table.getSelectionModel().isSelectionEmpty()));

        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setBackground(new Color(0xF2F2F2));
        header.setForeground(Color.DARK_GRAY);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(5).setCellRenderer(centered);
        table.getColumnModel().getColumn(6).setCellRenderer(centered);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel lblCancelHint = new JLabel("Select a row in the table and click \"Cancel Selected\" to remove it.");
        lblCancelHint.setBorder(new EmptyBorder(6, 4, 0, 4));
        tableWrapper.add(lblCancelHint, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);

        btnBook.addActionListener(this::onBook);
        btnClear.addActionListener(e -> clearForm());
        btnLoadAll.addActionListener(e -> loadAll());
        btnLoadByPatient.addActionListener(this::onLoadByPatient);
        btnCancelSelected.addActionListener(e -> cancelSelected());
        btnToggleAll.addItemListener(e -> refreshData());
    }

    private static int addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field){
        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; p.add(new JLabel(label), gbc);
        gbc.gridx=1; gbc.weightx=1; p.add(field, gbc);
        return row+1;
    }

    private void setupDatePicker() {
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spDate, "MMM dd, yyyy");
        spDate.setEditor(editor);
        spDate.setPreferredSize(new Dimension(140, spDate.getPreferredSize().height));
    }

    private void setupTimePicker() {
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spTime, "hh:mm a");
        spTime.setEditor(editor);
        spTime.setPreferredSize(new Dimension(120, spTime.getPreferredSize().height));
    }

    private void onBook(java.awt.event.ActionEvent e){
        JButton b = e.getSource() instanceof JButton ? (JButton) e.getSource() : null;
        if (b!=null) b.setEnabled(false);
        try { bookAppointment(); } finally { if (b!=null) b.setEnabled(true); }
    }

    private void onLoadByPatient(java.awt.event.ActionEvent e){
        String pid = tfPatientId.getText().trim();
        if (pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter a patient ID to load appointments."); return; }
        loadByPatient(pid);
    }

    private void bookAppointment() {
        resetValidation();
        try {
            String pid   = patientMode ? sessionPatientId : tfPatientId.getText().trim();
            String pname = tfPatientName.getText().trim();
            String prof  = tfProfessionalName.getText().trim();
            String type  = Objects.toString(cbType.getSelectedItem(),"");
            String reason= tfReason.getText().trim();
            int duration = (Integer) spDuration.getValue();

            boolean missing = false;
            if (!patientMode) missing |= markIfBlank(tfPatientId);
            missing |= markIfBlank(tfPatientName);
            missing |= markIfBlank(tfProfessionalName);
            if (missing) {
                JOptionPane.showMessageDialog(this, "Please complete the highlighted required fields.");
                return;
            }

            commitSpinners();
            LocalDate date = toLocalDate(spDate.getValue());
            LocalTime time = toLocalTime(spTime.getValue());
            var dto = CalendarDTO.newFromUI(pid, pname, prof, type, LocalDateTime.of(date, time), reason, duration);

            boolean ok = dispatch.bookAppointment(dto);
            if (ok) {
                JOptionPane.showMessageDialog(this, confirmationText(dto));
                clearForm();
                if (patientMode) loadByPatient(pid); else loadAll();
            }
            else { JOptionPane.showMessageDialog(this, "Booking failed.", "Error", JOptionPane.ERROR_MESSAGE); }
        } catch (Exception ex) {
            markInvalid(spinnerField(spDate));
            markInvalid(spinnerField(spTime));
            JOptionPane.showMessageDialog(this, "Please provide a valid date and time (e.g., Mar 10, 2025 at 2:30 PM).", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAll(){
        reloadAction = this::loadAllInternal;
        loadAllInternal();
    }

    private void loadAllInternal(){
        if (patientMode) { loadByPatientInternal(sessionPatientId); return; }
        renderFiltered(dispatch.listAllAppointments());
    }

    private void loadByPatient(String pid){
        String trimmed = pid == null ? "" : pid.trim();
        reloadAction = () -> loadByPatientInternal(trimmed);
        loadByPatientInternal(trimmed);
    }

    private void loadByPatientInternal(String pid){
        renderFiltered(dispatch.listAppointmentsByPatient(pid));
    }

    private void cancelSelected(){
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row to cancel."); return; }
        String idText = String.valueOf(model.getValueAt(row, 0));
        try {
            UUID id = UUID.fromString(idText);
            boolean ok = dispatch.cancelAppointment(id);
            if (ok) {
                String summary = "Cancelled appointment " + idText + " for " + model.getValueAt(row,2)
                        + " on " + model.getValueAt(row,5);
                JOptionPane.showMessageDialog(this, summary, "Appointment cancelled", JOptionPane.INFORMATION_MESSAGE);
                if (patientMode) loadByPatient(sessionPatientId); else loadAll();
            }
            else { JOptionPane.showMessageDialog(this, "Cancel failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);}
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "The appointment ID is not valid.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderFiltered(List<CalendarDTO> list){
        LocalDateTime now = LocalDateTime.now();
        List<CalendarDTO> filtered = list.stream()
                .filter(a -> btnToggleAll.isSelected() || (a.appointmentTime() != null && !a.appointmentTime().isBefore(now)))
                .sorted((a,b) -> {
                    LocalDateTime ta = a.appointmentTime();
                    LocalDateTime tb = b.appointmentTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return ta.compareTo(tb);
                })
                .toList();

        model.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        for (CalendarDTO a : filtered) {
            model.addRow(new Object[]{
                    String.valueOf(a.id()),
                    nz(a.patientId()), nz(a.patientName()), nz(a.professionalName()), nz(a.professionalType()),
                    a.appointmentTime()==null?"":a.appointmentTime().format(fmt),
                    a.durationMinutes() + " minutes",
                    nz(a.reason())
            });
        }
    }

    private void refreshData(){ if (reloadAction != null) reloadAction.run(); }

    private void clearForm(){
        if (!patientMode) tfPatientId.setText("");
        if (patientMode && !sessionPatientName.isBlank()) tfPatientName.setText(sessionPatientName); else tfPatientName.setText("");
        tfProfessionalName.setText(""); cbType.setSelectedIndex(0);
        spDate.setValue(new Date()); spTime.setValue(new Date()); spDuration.setValue(30); tfReason.setText("");
        resetValidation();
    }

    private static String nz(String s){ return s==null?"":s; }

    private void resetValidation(){
        tfPatientId.setBorder(defaultFieldBorder);
        tfPatientName.setBorder(defaultFieldBorder);
        tfProfessionalName.setBorder(defaultFieldBorder);
        setBorder(spinnerField(spDate), defaultFieldBorder);
        setBorder(spinnerField(spTime), defaultFieldBorder);
    }

    private boolean markIfBlank(JTextField tf){
        if (tf.getText().trim().isEmpty()) { markInvalid(tf); return true; }
        return false;
    }

    private void markInvalid(JComponent c){ setBorder(c, BorderFactory.createLineBorder(Color.RED)); }

    private void setBorder(JComponent c, Border border){ if (c != null) c.setBorder(border); }

    private void commitSpinners() throws ParseException { spDate.commitEdit(); spTime.commitEdit(); }

    private static JFormattedTextField spinnerField(JSpinner spinner){
        if (spinner == null) return null;
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) return de.getTextField();
        return null;
    }

    private static LocalDate toLocalDate(Object value){
        if (value instanceof Date d) return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        throw new IllegalArgumentException("Invalid date value");
    }

    private static LocalTime toLocalTime(Object value){
        if (value instanceof Date d) return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
        throw new IllegalArgumentException("Invalid time value");
    }

    private String bannerText() {
        if (sessionPatientName == null || sessionPatientName.isBlank()) return "Patient ID " + sessionPatientId;
        return sessionPatientName + " (" + sessionPatientId + ")";
    }

    private String confirmationText(CalendarDTO dto) {
        StringBuilder sb = new StringBuilder("<html><h3>Appointment booked</h3><ul>");
        sb.append("<li>For: ").append(patientDisplay(dto)).append("</li>");
        sb.append("<li>With: ").append(dto.professionalName()).append(" (" + dto.professionalType() + ")</li>");
        if (dto.appointmentTime() != null) {
            sb.append("<li>When: ").append(dto.appointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("</li>");
        }
        sb.append("<li>Duration: ").append(dto.durationMinutes()).append(" minutes</li>");
        if (dto.reason() != null && !dto.reason().isBlank()) {
            sb.append("<li>Reason: ").append(dto.reason()).append("</li>");
        }
        sb.append("</ul></html>");
        return sb.toString();
    }

    private String patientDisplay(CalendarDTO dto) {
        if (patientMode && sessionPatientId != null) return bannerText();
        if (dto.patientName() != null && !dto.patientName().isBlank()) {
            return dto.patientName() + " (" + dto.patientId() + ")";
        }
        return "Patient ID " + dto.patientId();
    }
}