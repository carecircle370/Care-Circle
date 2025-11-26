package com.carecircle.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;

/** Shared high-contrast styling helpers for Swing panels. */
public final class UiStyles {
    public static final Color TEXT_COLOR = new Color(24, 24, 24);
    public static final Color BORDER_COLOR = new Color(45, 45, 45);
    public static final Color ACCENT_COLOR = new Color(33, 119, 200);
    public static final Color HINT_COLOR = new Color(88, 88, 88);
    public static final Color PANEL_BG = new Color(249, 249, 249);
    public static final Color ALT_ROW_BG = new Color(234, 240, 247);
    public static final Color HEADER_BG = new Color(215, 229, 244);
    public static final Color ERROR_COLOR = new Color(176, 0, 32);

    public static final Font BASE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font STRONG_FONT = BASE_FONT.deriveFont(Font.BOLD, 15f);
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 15);
    public static final Font HINT_FONT = BASE_FONT.deriveFont(Font.ITALIC, 12f);

    private UiStyles() { }

    public static JLabel makeLabel(String text, boolean required) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(required ? STRONG_FONT : BASE_FONT);
        lbl.setForeground(TEXT_COLOR);
        return lbl;
    }

    public static JLabel hintLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HINT_FONT);
        lbl.setForeground(HINT_COLOR);
        return lbl;
    }

    public static void applyFieldStyle(JComponent comp) {
        comp.setFont(BASE_FONT);
        comp.setForeground(TEXT_COLOR);
        comp.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(6, 8, 6, 8)));
        if (comp instanceof JTextComponent text) {
            text.setCaretColor(TEXT_COLOR);
        }
    }

    public static void applyTextAreaStyle(JTextArea area) {
        area.setFont(BASE_FONT);
        area.setForeground(TEXT_COLOR);
        area.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(6, 8, 6, 8)));
    }

    public static void applyButtonStyle(AbstractButton button) {
        button.setFont(STRONG_FONT);
        button.setForeground(TEXT_COLOR);
    }

    public static void applyDialogTheme() {
        UIManager.put("OptionPane.background", PANEL_BG);
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
        UIManager.put("OptionPane.messageFont", BASE_FONT);
    }

    public static LineBorder errorBorder() {
        return new LineBorder(ERROR_COLOR, 2, true);
    }

    public static void styleTable(JTable table) {
        table.setFont(BASE_FONT);
        table.setForeground(TEXT_COLOR);
        table.setRowHeight(26);
        table.setGridColor(BORDER_COLOR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setBackground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setForeground(TEXT_COLOR);
        header.setBackground(HEADER_BG);
        header.setOpaque(true);

        DefaultTableCellRenderer renderer = new HighContrastTableCellRenderer(SwingConstants.LEFT);
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static DefaultTableCellRenderer highContrastCenteredRenderer() {
        return new HighContrastTableCellRenderer(SwingConstants.CENTER);
    }

    private static final class HighContrastTableCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;

        private HighContrastTableCellRenderer(int alignment) {
            this.alignment = alignment;
            setOpaque(true);
            setForeground(TEXT_COLOR);
            setFont(BASE_FONT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel lbl) {
                lbl.setHorizontalAlignment(alignment);
                lbl.setFont(BASE_FONT);
                lbl.setForeground(TEXT_COLOR);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
                if (!isSelected) {
                    lbl.setBackground((row % 2 == 0) ? Color.WHITE : ALT_ROW_BG);
                }
            }
            if (isSelected) {
                c.setBackground(ACCENT_COLOR);
                c.setForeground(Color.WHITE);
            }
            return c;
        }
    }
}