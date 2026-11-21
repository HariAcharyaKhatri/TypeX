package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;
import com.typex.util.IconManager;

import javax.swing.*;
import java.awt.*;

public class AboutPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final JLabel logo = new JLabel();
    private final JLabel title = new JLabel("TYPEX");
    private final JLabel tag = new JLabel("Master Every Keystroke.");
    private final JLabel desc = new JLabel();

    public AboutPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(8, 6, 8, 6);

        logo.setIcon(IconManager.getIcon("keyboard", 54, ThemeManager.accent()));
        add(logo, gc);

        gc.gridy++;
        title.setFont(title.getFont().deriveFont(Font.BOLD, 46f));
        add(title, gc);

        gc.gridy++;
        tag.setFont(tag.getFont().deriveFont(Font.PLAIN, 18f));
        add(tag, gc);

        gc.gridy++;
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 14f));
        desc.setText("<html><div style='width:420px;text-align:center;line-height:1.6'>" +
                "A modern, offline-first typing tutor built with Java Swing.<br>" +
                "Track speed, build muscle memory, and conquer keyboard drills.<br><br>" +
                "Version 1.0.0 · © 2024 TYPEX</div></html>");
        add(desc, gc);

        applyTheme();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        logo.setIcon(IconManager.getIcon("keyboard", 54, ThemeManager.accent()));
        title.setForeground(ThemeManager.accent());
        tag.setForeground(ThemeManager.fgPrimary());
        desc.setForeground(ThemeManager.fgMuted());
        repaint();
    }

    @Override
    public void refresh() {}
}
