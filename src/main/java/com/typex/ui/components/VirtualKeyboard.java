package com.typex.ui.components;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VirtualKeyboard extends JPanel {
    private static final String[] ROWS = {
            "`1234567890-=",
            "qwertyuiop[]\\",
            "asdfghjkl;'",
            "zxcvbnm,./"
    };
    private static final Map<Character, Color> FINGER_COLORS = new HashMap<>();
    static {
        Color[] c = {
            new Color(0xF87171), new Color(0xFBBF24), new Color(0x34D399),
            new Color(0x60A5FA), new Color(0x60A5FA), new Color(0x34D399),
            new Color(0xFBBF24), new Color(0xF87171)
        };
        String home = "asdfjkl;";
        for (int i = 0; i < home.length(); i++) {
            FINGER_COLORS.put(home.charAt(i), c[i]);
        }
    }

    private final Map<Character, JButton> keys = new HashMap<>();
    private char highlight = 0;

    public VirtualKeyboard() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (String row : ROWS) {
            JPanel rp = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            rp.setOpaque(false);
            for (char ch : row.toCharArray()) {
                JButton b = makeKey(ch);
                keys.put(ch, b);
                rp.add(b);
            }
            add(rp);
        }
        JPanel spaceRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        spaceRow.setOpaque(false);
        JButton space = makeKey(' ');
        space.setPreferredSize(new Dimension(320, 36));
        space.setText("space");
        keys.put(' ', space);
        spaceRow.add(space);
        add(spaceRow);

        applyTheme();
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        for (Map.Entry<Character, JButton> entry : keys.entrySet()) {
            char ch = entry.getKey();
            JButton b = entry.getValue();
            b.setBackground(ThemeManager.bgCard());
            b.setForeground(ThemeManager.fgPrimary());
            updateKeyBorder(ch, b);
        }
        if (highlight != 0) {
            highlightKey(highlight);
        }
        repaint();
    }

    private void updateKeyBorder(char ch, JButton b) {
        Color finger = FINGER_COLORS.get(Character.toLowerCase(ch));
        if (finger != null) {
            b.setBorder(BorderFactory.createLineBorder(finger, 2));
        } else {
            b.setBorder(BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xCCCCCC), 1));
        }
    }

    private JButton makeKey(char ch) {
        JButton b = new JButton(String.valueOf(ch).toUpperCase());
        b.setPreferredSize(new Dimension(42, 40));
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        updateKeyBorder(ch, b);
        return b;
    }

    public void highlightKey(char ch) {
        char old = highlight;
        if (old != 0 && keys.containsKey(old)) {
            updateKeyBorder(old, keys.get(old));
        }
        highlight = Character.toLowerCase(ch);
        if (keys.containsKey(highlight)) {
            JButton b = keys.get(highlight);
            b.setBorder(BorderFactory.createLineBorder(ThemeManager.accent(), 3));
        }
    }

    public void flashWrong(char ch) {
        char c = Character.toLowerCase(ch);
        if (keys.containsKey(c)) {
            JButton b = keys.get(c);
            Color orig = b.getBackground();
            b.setBackground(new Color(0xEF4444));
            Timer t = new Timer(180, e -> b.setBackground(orig));
            t.setRepeats(false);
            t.start();
        }
    }
}
