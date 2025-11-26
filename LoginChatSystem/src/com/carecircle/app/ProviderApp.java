package com.carecircle.app;

import com.carecircle.core.Dispatchers;
import com.carecircle.ui.VitalsViewerPanel;
import com.carecircle.ui.UpcomingAppointmentsPanel;
import com.carecircle.ui.ProviderPatientsPanel;

import javax.swing.*;

/** Provider portal with provider-scoped access (assignments persisted to CSV). */
public final class ProviderApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var frame = new JFrame("CareCircle – Provider Portal");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(980, 720);
            frame.setLocationRelativeTo(null);

            String providerId = JOptionPane.showInputDialog(frame, "Enter Provider ID:");
            if (providerId == null || providerId.isBlank()) { JOptionPane.showMessageDialog(frame, "Provider ID required."); return; }

            var tabs = new JTabbedPane();
            tabs.addTab("Manage Patients", new ProviderPatientsPanel(providerId));
            tabs.addTab("Vitals (View)", new VitalsViewerPanel(
                    Dispatchers.Factory.vitalsForProvider(providerId),
                    "",
                    "",
                    "Results are limited to patients assigned to you.",
                    true));
            tabs.addTab("Upcoming Appointments", new UpcomingAppointmentsPanel(Dispatchers.Factory.calendarForProvider(providerId)));

            frame.setContentPane(tabs);
            frame.setVisible(true);
        });
    }
}