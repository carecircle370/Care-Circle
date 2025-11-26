package com.carecircle.app;

import com.carecircle.core.Dispatchers;
import com.carecircle.ui.AppointmentsPanel;
import com.carecircle.ui.VitalsSubmitPanel;
import com.carecircle.ui.VitalsViewerPanel;

import javax.swing.*;
import java.awt.*;

/** Patient portal with per-window scope. */
public final class PatientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var frame = new JFrame("CareCircle – Patient Portal");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(980, 720);
            frame.setLocationRelativeTo(null);

            String pid = JOptionPane.showInputDialog(frame, "Enter your Patient ID:");
            if (pid == null || pid.isBlank()) { JOptionPane.showMessageDialog(frame, "Patient ID required."); return; }
            String pname = JOptionPane.showInputDialog(frame, "Enter your name (optional):");
            if (pname == null) pname = "";

            var tabs = new JTabbedPane();
            tabs.addTab("Vitals (Submit)", new VitalsSubmitPanel(pid.trim(), pname.trim()));
            tabs.addTab("Vitals (View)", new VitalsViewerPanel(Dispatchers.Factory.vitalsForPatient(pid), pid.trim(), pname.trim()));
            tabs.addTab("Appointments", new AppointmentsPanel(Dispatchers.Factory.calendarForPatient(pid), pid.trim(), pname.trim()));

            tabs.setTabComponentAt(0, createTabHeader("Vitals (Submit)", "Submit today's vitals."));
            tabs.setTabComponentAt(1, createTabHeader("Vitals (View)", "See your past vitals."));
            tabs.setTabComponentAt(2, createTabHeader("Appointments", "Book or review appointments."));

            tabs.setToolTipTextAt(0, "Send in your current vitals for your care team.");
            tabs.setToolTipTextAt(1, "Review previously submitted vitals and trends.");
            tabs.setToolTipTextAt(2, "Schedule or check upcoming appointments.");

            frame.setContentPane(tabs);
            frame.setVisible(true);
        });
    }

    private static JComponent createTabHeader(String title, String description) {
        var container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        var titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        var descLabel = new JLabel(description);
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, titleLabel.getFont().getSize2D() - 2));
        descLabel.setForeground(Color.DARK_GRAY);

        container.add(titleLabel);
        container.add(descLabel);

        return container;
    }
}