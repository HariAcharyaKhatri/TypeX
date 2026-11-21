package com.typex.ui.components;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class StatCard extends JPanel {
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel titleLabel = new JLabel();
    private final JLabel iconLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();

    public StatCard(String title, String value) {
        this(title, value, null, null);
    }

    public StatCard(String title, String value, Icon icon) {
        this(title, value, null, icon);
    }

    public StatCard(String title, String value, String subtitle, Icon icon) {
        setLayout(new BorderLayout(4, 4));
        setPreferredSize(new Dimension(180, 88));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        titleLabel.setText(title);
        titleLabel.setFont(ThemeManager.getTitleFont(11.5f));
        header.add(titleLabel, BorderLayout.WEST);

        if (icon != null) {
            iconLabel.setIcon(icon);
            header.add(iconLabel, BorderLayout.EAST);
        }

        valueLabel.setText(value);
        valueLabel.setFont(ThemeManager.getTitleFont(22f));

        add(header, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);

        if (subtitle != null && !subtitle.isBlank()) {
            subtitleLabel.setText(subtitle);
            subtitleLabel.setFont(ThemeManager.getBodyFont(10.5f));
            add(subtitleLabel, BorderLayout.SOUTH);
        }

        applyTheme();
    }

    public void setCardIcon(Icon icon) {
        iconLabel.setIcon(icon);
        iconLabel.setVisible(icon != null);
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        titleLabel.setForeground(ThemeManager.fgMuted());
        valueLabel.setForeground(ThemeManager.accent());
        subtitleLabel.setForeground(ThemeManager.fgMuted());
        repaint();
    }

    public void setValue(String v) {
        valueLabel.setText(v);
    }
}
