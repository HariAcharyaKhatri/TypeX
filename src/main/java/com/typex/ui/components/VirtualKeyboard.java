package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.util.KeyboardLayout;
import com.typex.util.KeyboardLayout.Finger;
import com.typex.util.KeyboardLayout.KeyDef;
import com.typex.util.KeyboardLayout.LayoutInfo;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Full-Size, Proportional Virtual Keyboard for TYPEX.
 * Adapts to available width, supports dynamic layouts, finger guidance,
 * shifted modifiers, heatmaps, and realistic physical key styling.
 */
public class VirtualKeyboard extends JPanel {
    private LayoutInfo activeLayout;
    private final Map<String, KeyComponent> keyComponentsById = new HashMap<>();
    private final Map<Character, KeyComponent> keyComponentsByChar = new HashMap<>();
    private KeyComponent leftShiftKey;
    private KeyComponent rightShiftKey;
    private KeyComponent spaceKey;

    private char activeHighlight = 0;
    private boolean heatmapMode = false;
    private Map<Character, Integer> errorCounts = new HashMap<>();

    public VirtualKeyboard() {
        this(KeyboardLayout.getLayout("US_QWERTY"));
    }

    public VirtualKeyboard(LayoutInfo layout) {
        this.activeLayout = layout != null ? layout : KeyboardLayout.getLayout("US_QWERTY");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setOpaque(false);
        buildKeyboardUI();
        applyTheme();
    }

    public void setKeyboardLayout(LayoutInfo layout) {
        if (layout == null || layout == this.activeLayout) return;
        this.activeLayout = layout;
        buildKeyboardUI();
        applyTheme();
    }

    public void setLayoutInfo(LayoutInfo layout) {
        setKeyboardLayout(layout);
    }

    public LayoutInfo getKeyboardLayout() {
        return activeLayout;
    }

    public LayoutInfo getLayoutInfo() {
        return activeLayout;
    }

    private void buildKeyboardUI() {
        removeAll();
        keyComponentsById.clear();
        keyComponentsByChar.clear();
        leftShiftKey = null;
        rightShiftKey = null;
        spaceKey = null;

        for (List<KeyDef> rowDefs : activeLayout.rows) {
            JPanel rowPanel = new KeyboardRowPanel();
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
            rowPanel.setOpaque(false);

            for (KeyDef def : rowDefs) {
                KeyComponent comp = new KeyComponent(def);
                keyComponentsById.put(def.id, comp);

                if ("LSHIFT".equalsIgnoreCase(def.id)) leftShiftKey = comp;
                if ("RSHIFT".equalsIgnoreCase(def.id)) rightShiftKey = comp;
                if ("SPACE".equalsIgnoreCase(def.id)) spaceKey = comp;

                if (def.primary != null && def.primary.length() == 1) {
                    char p = def.primary.charAt(0);
                    keyComponentsByChar.put(Character.toLowerCase(p), comp);
                    keyComponentsByChar.put(Character.toUpperCase(p), comp);
                }
                if (def.shifted != null && def.shifted.length() == 1) {
                    keyComponentsByChar.put(def.shifted.charAt(0), comp);
                }

                rowPanel.add(comp);
                rowPanel.add(Box.createHorizontalStrut(4));
            }
            add(rowPanel);
            add(Box.createVerticalStrut(4));
        }

        revalidate();
        repaint();
    }

    public static String getFingerInstruction(char c) {
        return KeyboardLayout.getFingerInstruction(KeyboardLayout.getLayout("US_QWERTY"), c);
    }

    public String getFingerInstructionForActiveLayout(char c) {
        return KeyboardLayout.getFingerInstruction(activeLayout != null ? activeLayout : KeyboardLayout.getLayout("US_QWERTY"), c);
    }

    public void highlightKey(char ch) {
        clearHighlights();
        activeHighlight = ch;

        if (ch == ' ') {
            if (spaceKey != null) spaceKey.setActive(true);
            return;
        }

        KeyComponent comp = keyComponentsByChar.get(ch);
        if (comp == null) {
            comp = keyComponentsByChar.get(Character.toLowerCase(ch));
        }

        if (comp != null) {
            comp.setActive(true);
            // Highlight shift key if required
            if (activeLayout.requiresShift(ch)) {
                Finger f = comp.keyDef.finger;
                boolean isLeftHand = f == Finger.LEFT_PINKY || f == Finger.LEFT_RING || f == Finger.LEFT_MIDDLE || f == Finger.LEFT_INDEX;
                if (isLeftHand && rightShiftKey != null) {
                    rightShiftKey.setActive(true);
                } else if (!isLeftHand && leftShiftKey != null) {
                    leftShiftKey.setActive(true);
                }
            }
        }
    }

    public void clearHighlights() {
        activeHighlight = 0;
        for (KeyComponent comp : keyComponentsById.values()) {
            comp.setActive(false);
        }
    }

    public void flashWrong(char ch) {
        KeyComponent comp = keyComponentsByChar.get(ch);
        if (comp == null) comp = keyComponentsByChar.get(Character.toLowerCase(ch));
        if (comp != null) {
            comp.flashError();
        }
    }

    public void setHeatmapMode(boolean enabled, Map<Character, Integer> errors) {
        this.heatmapMode = enabled;
        this.errorCounts = errors != null ? errors : new HashMap<>();
        applyTheme();
    }

    public boolean isHeatmapMode() {
        return heatmapMode;
    }

    public void applyTheme() {
        int maxErrors = errorCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (KeyComponent comp : keyComponentsById.values()) {
            if (heatmapMode) {
                int errs = 0;
                if (comp.keyDef.primary != null && comp.keyDef.primary.length() == 1) {
                    errs = errorCounts.getOrDefault(Character.toLowerCase(comp.keyDef.primary.charAt(0)), 0);
                }
                comp.setHeatmapIntensity(errs, maxErrors);
            } else {
                comp.clearHeatmap();
            }
            comp.applyTheme();
        }

        if (activeHighlight != 0) {
            highlightKey(activeHighlight);
        }
        repaint();
    }

    private static class KeyboardRowPanel extends JPanel {
        @Override
        public Dimension getMaximumSize() {
            Dimension d = super.getPreferredSize();
            d.width = Integer.MAX_VALUE;
            return d;
        }
    }

    /**
     * Individual realistic physical keyboard key with proportional width.
     */
    public static class KeyComponent extends JComponent {
        public final KeyDef keyDef;
        private boolean isActive = false;
        private boolean isFlashingError = false;
        private int heatmapCount = 0;
        private float heatmapRatio = 0.0f;

        public KeyComponent(KeyDef def) {
            this.keyDef = def;
            setOpaque(false);
            setFocusable(false);
            setToolTipText(def.finger != null ? def.finger.displayName : null);
        }

        public void setActive(boolean a) {
            this.isActive = a;
            repaint();
        }

        public void flashError() {
            this.isFlashingError = true;
            repaint();
            javax.swing.Timer t = new javax.swing.Timer(160, e -> {
                isFlashingError = false;
                repaint();
            });
            t.setRepeats(false);
            t.start();
        }

        public void setHeatmapIntensity(int count, int max) {
            this.heatmapCount = count;
            this.heatmapRatio = (count > 0 && max > 0) ? (float) count / max : 0.0f;
        }

        public void clearHeatmap() {
            this.heatmapCount = 0;
            this.heatmapRatio = 0.0f;
        }

        public void applyTheme() {
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int baseKeyWidth = 46;
            int baseKeyHeight = 44;
            int w = (int) Math.round(baseKeyWidth * keyDef.widthMultiplier);
            return new Dimension(w, baseKeyHeight);
        }

        @Override
        public Dimension getMinimumSize() {
            int baseKeyWidth = 34;
            int baseKeyHeight = 36;
            int w = (int) Math.round(baseKeyWidth * keyDef.widthMultiplier);
            return new Dimension(w, baseKeyHeight);
        }

        @Override
        public Dimension getMaximumSize() {
            int baseKeyWidth = 80;
            int baseKeyHeight = 52;
            int w = (int) Math.round(baseKeyWidth * keyDef.widthMultiplier);
            return new Dimension(w, baseKeyHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            Color bg;
            Color border;
            Color textPrimaryColor = ThemeManager.fgPrimary();
            Color textSecondaryColor = ThemeManager.fgMuted();

            if (isFlashingError) {
                bg = new Color(0xFEE2E2);
                border = new Color(0xDC2626);
                textPrimaryColor = new Color(0xDC2626);
            } else if (isActive) {
                bg = new Color(0xDBEAFE);
                border = ThemeManager.accent();
                textPrimaryColor = new Color(0x1D4ED8);
            } else if (heatmapRatio > 0.0f) {
                bg = interpolateHeat(heatmapRatio);
                border = new Color(0xCBD5E1);
                textPrimaryColor = (heatmapRatio > 0.6f) ? Color.WHITE : ThemeManager.fgPrimary();
            } else if (keyDef.isModifier) {
                bg = ThemeManager.isDark() ? new Color(0x1F2937) : new Color(0xF1F5F9);
                border = ThemeManager.isDark() ? new Color(0x374151) : new Color(0xE2E8F0);
                textPrimaryColor = ThemeManager.fgSecondary();
            } else {
                bg = ThemeManager.bgCard();
                border = ThemeManager.isDark() ? new Color(0x374151) : new Color(0xE2E8F0);
            }

            // Key shadow / 3D depth effect (subtle 1px bottom bevel)
            int arc = 8;
            g2.setColor(ThemeManager.isDark() ? new Color(0x111827) : new Color(0xCBD5E1));
            g2.fillRoundRect(0, 1, width, height - 1, arc, arc);

            // Key surface
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, width, height - 2, arc, arc);

            // Key border
            g2.setColor(border);
            g2.setStroke(new BasicStroke(isActive ? 2.0f : 1.0f));
            g2.drawRoundRect(0, 0, width - 1, height - 3, arc, arc);

            // Finger indicator strip (top subtle 2.5px colored line)
            if (keyDef.finger != null && !keyDef.isModifier && !keyDef.id.equals("SPACE")) {
                g2.setColor(keyDef.finger.color);
                g2.fillRect(arc / 2, 2, width - arc, 2);
            }

            // Draw labels
            Font baseFont = getFont();
            if (baseFont == null) baseFont = new Font("Segoe UI", Font.PLAIN, 12);

            FontMetrics fm = g2.getFontMetrics(baseFont);

            if (keyDef.shifted != null && !keyDef.shifted.isEmpty()) {
                // Secondary shifted label at top
                Font shiftFont = baseFont.deriveFont(Font.PLAIN, 10f);
                g2.setFont(shiftFont);
                g2.setColor(textSecondaryColor);
                FontMetrics sfm = g2.getFontMetrics(shiftFont);
                int sx = 6;
                int sy = sfm.getAscent() + 4;
                g2.drawString(keyDef.shifted, sx, sy);

                // Primary label at bottom/center
                Font priFont = baseFont.deriveFont(Font.BOLD, 13f);
                g2.setFont(priFont);
                g2.setColor(textPrimaryColor);
                FontMetrics pfm = g2.getFontMetrics(priFont);
                String priStr = keyDef.primary.toUpperCase();
                int px = width - pfm.stringWidth(priStr) - 8;
                int py = height - 8;
                g2.drawString(priStr, px, py);
            } else {
                // Centered single label
                Font mainFont = keyDef.isModifier
                        ? baseFont.deriveFont(Font.PLAIN, 11f)
                        : baseFont.deriveFont(Font.BOLD, 13f);
                g2.setFont(mainFont);
                g2.setColor(textPrimaryColor);
                FontMetrics mfm = g2.getFontMetrics(mainFont);

                String label = keyDef.primary != null ? keyDef.primary : keyDef.id;
                int lx = (width - mfm.stringWidth(label)) / 2;
                int ly = (height - 2 + mfm.getAscent() - mfm.getDescent()) / 2;
                g2.drawString(label, Math.max(3, lx), ly);
            }

            g2.dispose();
        }

        private Color interpolateHeat(float ratio) {
            if (ratio < 0.35f) {
                return new Color(0xFE, 0xF3, 0xC7); // amber light
            } else if (ratio < 0.70f) {
                return new Color(0xFB, 0x92, 0x3C); // vivid orange
            } else {
                return new Color(0xEF, 0x44, 0x44); // crimson red
            }
        }
    }
}
