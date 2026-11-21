package com.typex.ui.components;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class StatCard extends JPanel {
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel titleLabel = new JLabel();

    public StatCard(String title, String value) {
        setLayout(new BorderLayout(4, 6));
        setPreferredSize(new Dimension(180, 88));

        titleLabel.setText(title);
        titleLabel.setFont(ThemeManager.getTitleFont(11.5f));

        valueLabel.setText(value);
        valueLabel.setFont(ThemeManager.getTitleFont(22f));

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        applyTheme();
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        titleLabel.setForeground(ThemeManager.fgMuted());
        valueLabel.setForeground(ThemeManager.accent());
        repaint();
    }

    public void setValue(String v) {
        valueLabel.setText(v);
    }
}
