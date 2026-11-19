package com.typex.ui.components;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends JPanel {
    private static final String[][] ITEMS = {
            {"dashboard", "Dashboard", "🏠"},
            {"learn", "Learn Typing", "📚"},
            {"test", "Typing Test", "⌨"},
            {"custom", "Custom Practice", "✎"},
            {"challenge", "Speed Challenge", "⚡"},
            {"stats", "Statistics", "📊"},
            {"achievements", "Achievements", "🏆"},
            {"keyboard", "Keyboard Trainer", "⌨"},
            {"settings", "Settings", "⚙"},
            {"about", "About", "ℹ"}
    };

    private final Consumer<String> onNavigate;
    private final Map<String, JButton> buttons = new java.util.LinkedHashMap<>();
    private final JLabel brand = new JLabel("TYPEX");
    private final JLabel tag = new JLabel("Master Every Keystroke");
    private String activeKey = "dashboard";

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        brand.setFont(new Font("SansSerif", Font.BOLD, 24));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(brand);

        tag.setFont(tag.getFont().deriveFont(Font.PLAIN, 11f));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(tag);
        add(Box.createVerticalStrut(20));

        for (String[] it : ITEMS) {
            JButton b = createButton(it[1], it[2]);
            String key = it[0];
            b.addActionListener(e -> onNavigate.accept(key));
            buttons.put(key, b);
            add(b);
            add(Box.createVerticalStrut(4));
        }

        applyTheme();
    }

    public void applyTheme() {
        Color sidebarBg = ThemeManager.isDark() ? new Color(0x0E0E0E) : Color.WHITE;
        setBackground(sidebarBg);
        brand.setForeground(ThemeManager.accent());
        tag.setForeground(ThemeManager.fgMuted());

        for (var e : buttons.entrySet()) {
            boolean active = e.getKey().equals(activeKey);
            JButton b = e.getValue();
            b.setBackground(active ? ThemeManager.accent() : sidebarBg);
            b.setForeground(active ? Color.BLACK : ThemeManager.fgPrimary());
        }
        repaint();
    }

    private JButton createButton(String label, String icon) {
        JButton b = new JButton(icon + "  " + label);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 14f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return b;
    }

    public void setActive(String key) {
        this.activeKey = key;
        Color sidebarBg = ThemeManager.isDark() ? new Color(0x0E0E0E) : Color.WHITE;
        for (var e : buttons.entrySet()) {
            boolean active = e.getKey().equals(key);
            JButton b = e.getValue();
            b.setBackground(active ? ThemeManager.accent() : sidebarBg);
            b.setForeground(active ? Color.BLACK : ThemeManager.fgPrimary());
        }
    }
}
