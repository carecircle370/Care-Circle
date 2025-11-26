package com.carecircle.ui;

import com.carecircle.core.Dispatchers;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * UI for provider to manage assigned patients.
 * Discovery reads the raw CSVs (vitals.csv & appointments.csv) so providers can
 * find unassigned patients and add them to their roster.
 */
public final class ProviderPatientsPanel extends JPanel {
    private final String providerId;
    private final Dispatchers.ProviderAccessControl accessControl;

    private final DefaultListModel<String> allPatientsModel = new DefaultListModel<>();
    private final JList<String> allPatientsList = new JList<>(allPatientsModel);

    private final DefaultListModel<String> assignedPatientsModel = new DefaultListModel<>();
    private final JList<String> assignedPatientsList = new JList<>(assignedPatientsModel);

    private final JButton btnAssign = new JButton("Assign >>");
    private final JButton btnUnassign = new JButton("<< Unassign");
    private final JButton btnReload = new JButton("Reload Lists");

    public ProviderPatientsPanel(String providerId) {
        this.providerId = Objects.requireNonNull(providerId);
        this.accessControl = Dispatchers.Factory.accessControl();

        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(12,12,12,12));

        JLabel title = new JLabel("Manage Assigned Patients");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        JPanel left = new JPanel(new BorderLayout(6,6));
        left.add(new JLabel("Discovered Patients (not necessarily assigned)"), BorderLayout.NORTH);
        left.add(new JScrollPane(allPatientsList), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(6,6));
        right.add(new JLabel("Assigned Patients"), BorderLayout.NORTH);
        right.add(new JScrollPane(assignedPatientsList), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);

        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bottom.add(btnAssign);
        bottom.add(btnUnassign);
        bottom.add(btnReload);
        add(bottom, BorderLayout.SOUTH);

        btnAssign.addActionListener(e -> assignSelected());
        btnUnassign.addActionListener(e -> unassignSelected());
        btnReload.addActionListener(e -> reloadPatientLists());

        // double-click convenience: assign/unassign
        allPatientsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) assignSelected();
            }
        });
        assignedPatientsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) unassignSelected();
            }
        });

        reloadPatientLists();
    }

    private void reloadPatientLists() {
        allPatientsModel.clear();
        assignedPatientsModel.clear();

        // Discover patients by scanning vitals.csv and appointments.csv from current working dir.
        Set<String> discovered = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        discovered.addAll(discoverPatientIdsFrom("vitals.csv", 0));        // column 0 = patientId
        discovered.addAll(discoverPatientIdsFrom("appointments.csv", 1));  // column 1 = patientId in CalendarDTO rows

        // include currently assigned ones as well (in case provider_access.csv contains ids not present in CSVs yet)
        Set<String> assigned = new HashSet<>(accessControl.patientsFor(providerId));
        discovered.addAll(assigned);

        List<String> sorted = discovered.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();

        for (String pid : sorted) {
            if (assigned.contains(pid)) assignedPatientsModel.addElement(pid);
            else allPatientsModel.addElement(pid);
        }
    }

    // Read CSV file and pick the value at column index 'colIndex' for each row.
    // We ignore header row and any malformed lines.
    private static Set<String> discoverPatientIdsFrom(String fileName, int colIndex) {
        Set<String> out = new HashSet<>();
        File f = new File(fileName);
        if (!f.exists() || !f.isFile()) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    List<String> cols = Dispatchers.csvSplit(line);
                    if (cols.size() > colIndex) {
                        String v = cols.get(colIndex);
                        if (v != null && !v.isBlank()) out.add(v.trim());
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
        return out;
    }

    private void assignSelected() {
        java.util.List<String> selected = allPatientsList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        for (String pid : selected) {
            accessControl.assign(providerId, pid);
        }
        reloadPatientLists();
    }

    private void unassignSelected() {
        java.util.List<String> selected = assignedPatientsList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        for (String pid : selected) {
            accessControl.unassign(providerId, pid);
        }
        reloadPatientLists();
    }
}