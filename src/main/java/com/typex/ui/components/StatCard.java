package com.typex.ui.components;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class StatCard extends JPanel {
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel titleLabel = new JLabel();

    public StatCard(String title, String value) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(180, 90));

        titleLabel.setText(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));

        valueLabel.setText(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 24f));

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        applyTheme();
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x2E2E2E) : new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        titleLabel.setForeground(ThemeManager.fgMuted());
        valueLabel.setForeground(ThemeManager.accent());
        repaint();
    }

    public void setValue(String v) {
        valueLabel.setText(v);
    }
}
