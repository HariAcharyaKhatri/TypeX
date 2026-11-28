package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends JPanel {
    private static final String[][] CORE_ITEMS = {
            {"dashboard", "Dashboard", IconManager.DASHBOARD},
            {"test", "Typing Test", IconManager.TYPING_TEST},
            {"learn", "Learn Typing", IconManager.LEARN},
            {"games", "Typing Games", IconManager.GAMES},
            {"keyboard", "Keyboard Trainer", IconManager.KEYBOARD},
            {"stats", "Statistics", IconManager.STATISTICS}
    };

    private static final String[][] SECONDARY_ITEMS = {
            {"custom", "Custom Practice", IconManager.CUSTOM},
            {"challenge", "Speed Challenge", IconManager.CHALLENGE},
            {"achievements", "Achievements", IconManager.ACHIEVEMENTS},
            {"settings", "Settings", IconManager.SETTINGS},
            {"about", "About", IconManager.ABOUT}
    };

    private final Consumer<String> onNavigate;
    private final Map<String, NavButton> buttons = new LinkedHashMap<>();
    private final JLabel brand = new JLabel("TYPEX");
    private final JLabel tag = new JLabel("Master Every Keystroke");
    private final JLabel coreSectionLabel = new JLabel("MAIN");
    private final JLabel secSectionLabel = new JLabel("EXPLORE");
    private String activeKey = "dashboard";

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(236, 0));

        // Brand block with logo icon
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.setFont(ThemeManager.getTitleFont(22f));
        brand.setIcon(IconManager.getIcon(IconManager.KEYBOARD, 22, ThemeManager.accent()));
        brand.setIconTextGap(8);

        tag.setFont(ThemeManager.getBodyFont(11f));
        brandPanel.add(brand);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(tag);
        add(brandPanel);
        add(Box.createVerticalStrut(24));

        // Core section
        coreSectionLabel.setFont(ThemeManager.getTitleFont(10.5f));
        coreSectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(coreSectionLabel);
        add(Box.createVerticalStrut(6));

        for (String[] it : CORE_ITEMS) {
            NavButton b = createButton(it[0], it[1], it[2]);
            buttons.put(it[0], b);
            add(b);
            add(Box.createVerticalStrut(3));
        }

        add(Box.createVerticalStrut(14));

        // Secondary section
        secSectionLabel.setFont(ThemeManager.getTitleFont(10.5f));
        secSectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(secSectionLabel);
        add(Box.createVerticalStrut(6));

        for (String[] it : SECONDARY_ITEMS) {
            NavButton b = createButton(it[0], it[1], it[2]);
            buttons.put(it[0], b);
            add(b);
            add(Box.createVerticalStrut(3));
        }

        add(Box.createVerticalGlue());

        applyTheme();
    }

    public void applyTheme() {
        Color bg = ThemeManager.bgCard();
        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(18, 14, 18, 14)));

        brand.setForeground(ThemeManager.accent());
        brand.setIcon(IconManager.getIcon(IconManager.KEYBOARD, 22, ThemeManager.accent()));
        tag.setForeground(ThemeManager.fgMuted());
        coreSectionLabel.setForeground(ThemeManager.fgMuted());
        secSectionLabel.setForeground(ThemeManager.fgMuted());

        for (NavButton b : buttons.values()) {
            b.updateTheme();
        }
        repaint();
    }

    private NavButton createButton(String key, String label, String iconKey) {
        NavButton b = new NavButton(key, label, iconKey);
        b.addActionListener(e -> onNavigate.accept(key));
        return b;
    }

    public void setActive(String key) {
        this.activeKey = key;
        for (NavButton b : buttons.values()) {
            b.updateTheme();
        }
    }

    private class NavButton extends JButton {
        private final String key;
        private final String iconKey;
        private boolean isHovered = false;

        public NavButton(String key, String label, String iconKey) {
            super(label);
            this.key = key;
            this.iconKey = iconKey;
            setToolTipText(label);
            setHorizontalAlignment(SwingConstants.LEFT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setPreferredSize(new Dimension(208, 38));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
            setFont(ThemeManager.getBodyFont(13.5f));
            setIconTextGap(10);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    updateTheme();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    updateTheme();
                }
            });
        }

        public void updateTheme() {
            boolean active = key.equals(activeKey);
            if (active) {
                setBackground(ThemeManager.accentLight());
                setForeground(ThemeManager.accent());
                setIcon(IconManager.getIcon(iconKey, 18, ThemeManager.accent()));
                setFont(ThemeManager.getTitleFont(13.5f));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeManager.accent()),
                        BorderFactory.createEmptyBorder(7, 10, 7, 12)));
            } else if (isHovered) {
                setBackground(ThemeManager.bgSecondary());
                setForeground(ThemeManager.fgPrimary());
                setIcon(IconManager.getIcon(iconKey, 18, ThemeManager.fgPrimary()));
                setFont(ThemeManager.getBodyFont(13.5f));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0, 0, 0, 0)),
                        BorderFactory.createEmptyBorder(7, 10, 7, 12)));
            } else {
                setBackground(ThemeManager.bgCard());
                setForeground(ThemeManager.fgSecondary());
                setIcon(IconManager.getIcon(iconKey, 18, ThemeManager.fgSecondary()));
                setFont(ThemeManager.getBodyFont(13.5f));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0, 0, 0, 0)),
                        BorderFactory.createEmptyBorder(7, 10, 7, 12)));
            }
            repaint();
        }
    }
}
