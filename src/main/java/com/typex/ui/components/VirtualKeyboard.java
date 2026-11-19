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

    private static final Map<Character, String> FINGER_NAMES = new HashMap<>();
    private static final Map<Character, Color> FINGER_COLORS = new HashMap<>();

    static {
        // Finger color definitions
        Color leftPinky = new Color(0xF87171);
        Color leftRing = new Color(0xFB923C);
        Color leftMiddle = new Color(0xFBBF24);
        Color leftIndex = new Color(0x34D399);
        Color rightIndex = new Color(0x60A5FA);
        Color rightMiddle = new Color(0x818CF8);
        Color rightRing = new Color(0xA78BFA);
        Color rightPinky = new Color(0xF472B6);

        assignFinger("`1qaz", "Left Pinky Finger", leftPinky);
        assignFinger("2wsx", "Left Ring Finger", leftRing);
        assignFinger("3edc", "Left Middle Finger", leftMiddle);
        assignFinger("45rtfgvb", "Left Index Finger", leftIndex);
        assignFinger("67yuhjnm", "Right Index Finger", rightIndex);
        assignFinger("8ik,", "Right Middle Finger", rightMiddle);
        assignFinger("9ol.", "Right Ring Finger", rightRing);
        assignFinger("0-=[]\\;'/p", "Right Pinky Finger", rightPinky);
        FINGER_NAMES.put(' ', "Both Thumbs (Spacebar)");
    }

    private static void assignFinger(String keys, String name, Color c) {
        for (char ch : keys.toCharArray()) {
            FINGER_NAMES.put(ch, name);
            FINGER_COLORS.put(ch, c);
        }
    }

    private final Map<Character, JButton> keys = new HashMap<>();
    private char highlight = 0;
    private boolean heatmapMode = false;
    private Map<Character, Integer> errorCounts = new HashMap<>();

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

    public static String getFingerInstruction(char c) {
        char lower = Character.toLowerCase(c);
        return FINGER_NAMES.getOrDefault(lower, "Target Finger: Any Free Finger");
    }

    public void setHeatmapMode(boolean enabled, Map<Character, Integer> errors) {
        this.heatmapMode = enabled;
        this.errorCounts = (errors != null) ? errors : new HashMap<>();
        applyTheme();
    }

    public boolean isHeatmapMode() {
        return heatmapMode;
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        int maxErrors = errorCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (Map.Entry<Character, JButton> entry : keys.entrySet()) {
            char ch = entry.getKey();
            JButton b = entry.getValue();

            if (heatmapMode) {
                int errs = errorCounts.getOrDefault(ch, 0);
                if (errs == 0) {
                    b.setBackground(ThemeManager.bgCard());
                    b.setToolTipText(Character.toUpperCase(ch) + " (0 errors)");
                } else {
                    float ratio = (float) errs / Math.max(1, maxErrors);
                    Color heat = interpolateHeat(ratio);
                    b.setBackground(heat);
                    b.setToolTipText(String.format("%s (%d mistakes)", Character.toUpperCase(ch), errs));
                }
                b.setForeground(ThemeManager.isDark() ? Color.WHITE : Color.BLACK);
                b.setBorder(BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x444444) : new Color(0xBBBBBB), 1));
            } else {
                b.setBackground(ThemeManager.bgCard());
                b.setForeground(ThemeManager.fgPrimary());
                b.setToolTipText(FINGER_NAMES.getOrDefault(ch, "Key " + ch));
                updateKeyBorder(ch, b);
            }
        }

        if (highlight != 0) {
            highlightKey(highlight);
        }
        repaint();
    }

    private Color interpolateHeat(float ratio) {
        if (ratio < 0.33f) {
            return new Color(0x85, 0x76, 0x1A); // subtle gold
        } else if (ratio < 0.66f) {
            return new Color(0xC2, 0x64, 0x15); // amber orange
        } else {
            return new Color(0xB9, 0x1C, 0x1C); // crimson red
        }
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
            if (heatmapMode) {
                applyTheme();
            } else {
                updateKeyBorder(old, keys.get(old));
            }
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
