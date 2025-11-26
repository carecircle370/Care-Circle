package com.carecircle.ui;

import com.carecircle.core.Dispatchers.CalendarDispatch;
import com.carecircle.data.CalendarDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only list of upcoming appointments for providers. */
public final class UpcomingAppointmentsPanel extends JPanel {
    private final CalendarDispatch dispatch;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Patient ID", "Patient Name", "Professional", "Type", "Date/Time", "Duration", "Reason"
    }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final JTable table = new JTable(model);
    private final JButton btnRefresh = new JButton("Refresh");
    private final JLabel lblSummary = new JLabel(" ");

    public UpcomingAppointmentsPanel(CalendarDispatch dispatch) {
        this.dispatch = Objects.requireNonNull(dispatch);

        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(12,12,12,12));

        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(new EmptyBorder(0,0,6,0));

        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(5).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setMinWidth(90);

        JPanel north = new JPanel(new BorderLayout());
        north.add(title, BorderLayout.NORTH);
        north.add(btnRefresh, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        lblSummary.setBorder(new EmptyBorder(4,0,0,0));
        add(lblSummary, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> reload());

        reload();
    }

    private void reload() {
        List<CalendarDTO> upcoming = dispatch.listAllAppointments().stream()
                .filter(a -> a.appointmentTime() != null)
                .filter(a -> a.appointmentTime().atZone(ZoneId.systemDefault()).isAfter(java.time.ZonedDateTime.now()))
                .sorted(Comparator.comparing(CalendarDTO::appointmentTime))
                .collect(Collectors.toList());

        model.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy h:mm a");
        for (CalendarDTO a : upcoming) {
            model.addRow(new Object[]{
                    a.patientId(),
                    a.patientName(),
                    a.professionalName(),
                    a.professionalType(),
                    fmt.format(a.appointmentTime().atZone(ZoneId.systemDefault())),
                    a.durationMinutes() + " min",
                    a.reason()
            });
        }
        lblSummary.setText(upcoming.isEmpty() ? "No upcoming appointments." : ("Showing " + upcoming.size() + " appointment(s)."));
    }
}