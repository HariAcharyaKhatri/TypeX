package com.typex.util;

import com.typex.config.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Centralized, reliable vector icon system for TYPEX.
 * Renders crisp, scalable, theme-aware vector icons on HiDPI displays with zero
 * external font dependencies and zero missing classpath asset errors.
 */
public final class IconManager {

    private IconManager() {}

    /**
     * Common icon keys across TYPEX.
     */
    public static final String DASHBOARD = "dashboard";
    public static final String TYPING_TEST = "test";
    public static final String LEARN = "learn";
    public static final String GAMES = "games";
    public static final String KEYBOARD = "keyboard";
    public static final String STATISTICS = "stats";
    public static final String CUSTOM = "custom";
    public static final String CHALLENGE = "challenge";
    public static final String ACHIEVEMENTS = "achievements";
    public static final String SETTINGS = "settings";
    public static final String ABOUT = "about";
    public static final String PROFILE = "profile";

    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String RESTART = "restart";
    public static final String EXIT = "exit";
    public static final String CHECK = "check";
    public static final String LOCK = "lock";
    public static final String UNLOCK = "unlock";
    public static final String FLAME = "flame";
    public static final String STAR = "star";
    public static final String LIGHTBULB = "lightbulb";
    public static final String TIMER = "timer";
    public static final String SPEED = "speed";
    public static final String ACCURACY = "accuracy";
    public static final String SOUND_ON = "sound_on";
    public static final String SOUND_OFF = "sound_off";
    public static final String SOUND = "sound_on";
    public static final String THEME = "theme";
    public static final String HEART = "heart";
    public static final String ROCKET = "rocket";
    public static final String ALIEN = "alien";
    public static final String CODE = "code";
    public static final String HASH = "hash";
    public static final String TARGET = "target";
    public static final String TROPHY = "trophy";

    /**
     * Returns a crisp vector Icon scaled to the given size.
     */
    public static Icon getIcon(String key, int size, Color color) {
        String k = (key != null) ? key.toLowerCase().trim() : "";
        return new VectorIcon(k, size, color);
    }

    public static Icon getIcon(String key, int size) {
        return getIcon(key, size, null);
    }

    public static Icon getIcon(String key) {
        return getIcon(key, 18, null);
    }

    /**
     * Generates a multi-resolution application icon for taskbar and window frames.
     */
    public static List<Image> getAppIconImages() {
        int[] sizes = {16, 24, 32, 48, 64, 128};
        List<Image> list = new ArrayList<>();
        for (int s : sizes) {
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background rounded rectangle
            g2.setColor(new Color(0x2563EB));
            g2.fillRoundRect(1, 1, s - 2, s - 2, s / 3, s / 3);

            // Inner keyboard / 'TX' glyph
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Outer keycap outline
            int margin = Math.max(3, s / 5);
            g2.drawRoundRect(margin, margin, s - 2 * margin, s - 2 * margin, s / 6, s / 6);

            // T bar and stem
            int midX = s / 2;
            int topY = margin + (s - 2 * margin) / 4;
            int botY = margin + 3 * (s - 2 * margin) / 4;
            int arm = (s - 2 * margin) / 3;
            g2.drawLine(midX - arm, topY, midX + arm, topY);
            g2.drawLine(midX, topY, midX, botY);

            g2.dispose();
            list.add(img);
        }
        return list;
    }

    /**
     * Scalable Vector Icon implementing javax.swing.Icon.
     */
    public static class VectorIcon implements Icon {
        private final String key;
        private final int size;
        private final Color fixedColor;

        public VectorIcon(String key, int size, Color fixedColor) {
            this.key = key;
            this.size = Math.max(8, size);
            this.fixedColor = fixedColor;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color renderColor = fixedColor;
            if (renderColor == null) {
                renderColor = (c != null && c.getForeground() != null) ? c.getForeground() : ThemeManager.fgPrimary();
            }
            g2.setColor(renderColor);

            g2.translate(x, y);
            float scale = size / 24f;
            g2.scale(scale, scale);

            float strokeWidth = Math.max(1.5f, 2.0f);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            drawVector(key, g2, renderColor);

            g2.dispose();
        }

        private void drawVector(String k, Graphics2D g2, Color color) {
            switch (k) {
                case DASHBOARD -> {
                    // 4 clean rounded tiles
                    g2.fillRoundRect(3, 3, 7, 7, 2, 2);
                    g2.fillRoundRect(14, 3, 7, 7, 2, 2);
                    g2.fillRoundRect(3, 14, 7, 7, 2, 2);
                    g2.fillRoundRect(14, 14, 7, 7, 2, 2);
                }
                case TYPING_TEST, KEYBOARD -> {
                    // Keyboard outline
                    g2.drawRoundRect(2, 5, 20, 14, 3, 3);
                    // Spacebar
                    g2.drawLine(8, 15, 16, 15);
                    // Key dots
                    g2.fillOval(5, 8, 2, 2);
                    g2.fillOval(9, 8, 2, 2);
                    g2.fillOval(13, 8, 2, 2);
                    g2.fillOval(17, 8, 2, 2);
                    g2.fillOval(6, 11, 2, 2);
                    g2.fillOval(10, 11, 2, 2);
                    g2.fillOval(14, 11, 2, 2);
                    g2.fillOval(18, 11, 2, 2);
                }
                case LEARN -> {
                    // Open book
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 19);
                    p.curveTo(8, 16, 4, 16, 3, 17);
                    p.lineTo(3, 6);
                    p.curveTo(4, 5, 8, 5, 12, 8);
                    p.curveTo(16, 5, 20, 5, 21, 6);
                    p.lineTo(21, 17);
                    p.curveTo(20, 16, 16, 16, 12, 19);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(12, 8, 12, 19);
                }
                case GAMES -> {
                    // Arcade Gamepad Controller
                    g2.drawRoundRect(2, 6, 20, 12, 6, 6);
                    // D-Pad
                    g2.drawLine(6, 12, 10, 12);
                    g2.drawLine(8, 10, 8, 14);
                    // Action buttons
                    g2.fillOval(15, 10, 2, 2);
                    g2.fillOval(17, 12, 2, 2);
                }
                case STATISTICS -> {
                    // Bar chart with trend line
                    g2.drawLine(3, 20, 21, 20);
                    g2.drawRect(5, 13, 3, 7);
                    g2.drawRect(10, 9, 3, 11);
                    g2.drawRect(15, 5, 3, 15);
                    // Trend sparkline
                    g2.drawLine(4, 12, 9, 9);
                    g2.drawLine(9, 9, 14, 6);
                    g2.drawLine(14, 6, 19, 3);
                }
                case CUSTOM -> {
                    // Pencil
                    Path2D p = new Path2D.Float();
                    p.moveTo(18, 3);
                    p.lineTo(21, 6);
                    p.lineTo(9, 18);
                    p.lineTo(4, 20);
                    p.lineTo(6, 15);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(15, 6, 18, 9);
                }
                case CHALLENGE -> {
                    // Lightning bolt
                    Path2D p = new Path2D.Float();
                    p.moveTo(13, 2);
                    p.lineTo(5, 13);
                    p.lineTo(12, 13);
                    p.lineTo(10, 22);
                    p.lineTo(19, 10);
                    p.lineTo(12, 10);
                    p.closePath();
                    g2.fill(p);
                }
                case ACHIEVEMENTS, TROPHY -> {
                    // Trophy
                    g2.drawArc(6, 4, 12, 10, 0, -180);
                    g2.drawLine(6, 4, 18, 4);
                    g2.drawLine(12, 14, 12, 18);
                    g2.drawLine(8, 18, 16, 18);
                    // Handles
                    g2.drawArc(3, 5, 6, 6, 90, 180);
                    g2.drawArc(15, 5, 6, 6, -90, 180);
                }
                case SETTINGS -> {
                    // Gear
                    g2.drawOval(7, 7, 10, 10);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = (int) Math.round(12 + Math.cos(angle) * 7.5);
                        int y1 = (int) Math.round(12 + Math.sin(angle) * 7.5);
                        int x2 = (int) Math.round(12 + Math.cos(angle) * 10.5);
                        int y2 = (int) Math.round(12 + Math.sin(angle) * 10.5);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                case ABOUT -> {
                    // Circle with 'i'
                    g2.drawOval(3, 3, 18, 18);
                    g2.fillOval(11, 7, 2, 2);
                    g2.drawLine(12, 11, 12, 16);
                    g2.drawLine(11, 11, 12, 11);
                }
                case PROFILE -> {
                    // User avatar
                    g2.drawOval(8, 4, 8, 8);
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 20);
                    p.curveTo(5, 15, 8, 14, 12, 14);
                    p.curveTo(16, 14, 19, 15, 20, 20);
                    g2.draw(p);
                }
                case PLAY -> {
                    // Right-pointing triangle
                    Path2D p = new Path2D.Float();
                    p.moveTo(6, 4);
                    p.lineTo(19, 12);
                    p.lineTo(6, 20);
                    p.closePath();
                    g2.fill(p);
                }
                case PAUSE -> {
                    // Two pause bars
                    g2.fillRoundRect(6, 4, 4, 16, 2, 2);
                    g2.fillRoundRect(14, 4, 4, 16, 2, 2);
                }
                case RESTART -> {
                    // Circular refresh arrow
                    g2.drawArc(4, 4, 16, 16, 45, 270);
                    Path2D arrow = new Path2D.Float();
                    arrow.moveTo(18, 4);
                    arrow.lineTo(18, 9);
                    arrow.lineTo(13, 9);
                    arrow.closePath();
                    g2.fill(arrow);
                }
                case EXIT -> {
                    // Crisp cross
                    g2.drawLine(6, 6, 18, 18);
                    g2.drawLine(18, 6, 6, 18);
                }
                case CHECK -> {
                    // Checkmark
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 12);
                    p.lineTo(9, 17);
                    p.lineTo(20, 6);
                    g2.draw(p);
                }
                case LOCK -> {
                    // Padlock
                    g2.drawRoundRect(5, 10, 14, 11, 3, 3);
                    g2.drawArc(8, 4, 8, 10, 0, 180);
                    g2.fillOval(11, 14, 2, 3);
                }
                case UNLOCK -> {
                    // Open padlock
                    g2.drawRoundRect(5, 10, 14, 11, 3, 3);
                    g2.drawArc(10, 2, 8, 10, 0, 180);
                    g2.fillOval(11, 14, 2, 3);
                }
                case FLAME -> {
                    // Fire flame
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 2);
                    p.curveTo(13, 6, 17, 8, 17, 13);
                    p.curveTo(17, 18, 14, 21, 12, 21);
                    p.curveTo(8, 21, 5, 17, 5, 13);
                    p.curveTo(5, 9, 8, 7, 10, 5);
                    p.curveTo(10, 8, 12, 9, 12, 2);
                    p.closePath();
                    g2.fill(p);
                }
                case STAR -> {
                    // 5-point star
                    Path2D p = new Path2D.Float();
                    for (int i = 0; i < 10; i++) {
                        double angle = i * Math.PI / 5 - Math.PI / 2;
                        double r = (i % 2 == 0) ? 9.5 : 4.2;
                        double px = 12 + Math.cos(angle) * r;
                        double py = 12 + Math.sin(angle) * r;
                        if (i == 0) p.moveTo(px, py);
                        else p.lineTo(px, py);
                    }
                    p.closePath();
                    g2.fill(p);
                }
                case LIGHTBULB -> {
                    // Bulb
                    g2.drawArc(6, 3, 12, 12, 30, 290);
                    g2.drawLine(9, 18, 15, 18);
                    g2.drawLine(10, 21, 14, 21);
                }
                case TIMER -> {
                    // Clock with hands
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawLine(12, 7, 12, 12);
                    g2.drawLine(12, 12, 16, 14);
                }
                case SPEED -> {
                    // Speedometer
                    g2.drawArc(3, 4, 18, 18, 0, 180);
                    g2.drawLine(3, 13, 21, 13);
                    g2.drawLine(12, 13, 16, 8);
                }
                case ACCURACY, TARGET -> {
                    // Target bullseye
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawOval(7, 7, 10, 10);
                    g2.fillOval(11, 11, 2, 2);
                }
                case SOUND_ON -> {
                    // Speaker + sound waves
                    Path2D p = new Path2D.Float();
                    p.moveTo(3, 9);
                    p.lineTo(7, 9);
                    p.lineTo(12, 5);
                    p.lineTo(12, 19);
                    p.lineTo(7, 15);
                    p.lineTo(3, 15);
                    p.closePath();
                    g2.fill(p);
                    g2.drawArc(14, 7, 6, 10, -60, 120);
                }
                case SOUND_OFF -> {
                    // Speaker + X
                    Path2D p = new Path2D.Float();
                    p.moveTo(3, 9);
                    p.lineTo(7, 9);
                    p.lineTo(12, 5);
                    p.lineTo(12, 19);
                    p.lineTo(7, 15);
                    p.lineTo(3, 15);
                    p.closePath();
                    g2.fill(p);
                    g2.drawLine(15, 9, 20, 15);
                    g2.drawLine(20, 9, 15, 15);
                }
                case THEME -> {
                    // Moon / Sun
                    g2.drawOval(3, 3, 18, 18);
                    g2.fillArc(3, 3, 18, 18, 90, 180);
                }
                case HEART -> {
                    // Heart
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 20);
                    p.curveTo(6, 15, 3, 11, 3, 7.5);
                    p.curveTo(3, 4.5, 5.5, 3, 8.5, 3);
                    p.curveTo(10.5, 3, 11.5, 4, 12, 5);
                    p.curveTo(12.5, 4, 13.5, 3, 15.5, 3);
                    p.curveTo(18.5, 3, 21, 4.5, 21, 7.5);
                    p.curveTo(21, 11, 18, 15, 12, 20);
                    p.closePath();
                    g2.fill(p);
                }
                case ROCKET -> {
                    // Rocket
                    Path2D p = new Path2D.Float();
                    p.moveTo(12, 2);
                    p.curveTo(16, 6, 17, 12, 17, 17);
                    p.lineTo(15, 16);
                    p.lineTo(15, 19);
                    p.lineTo(9, 19);
                    p.lineTo(9, 16);
                    p.lineTo(7, 17);
                    p.curveTo(7, 12, 8, 6, 12, 2);
                    p.closePath();
                    g2.draw(p);
                    g2.fillOval(10, 8, 4, 4);
                }
                case ALIEN -> {
                    // Arcade invader
                    g2.drawRoundRect(4, 5, 16, 12, 4, 4);
                    g2.fillOval(7, 9, 3, 3);
                    g2.fillOval(14, 9, 3, 3);
                    g2.drawLine(4, 2, 7, 5);
                    g2.drawLine(20, 2, 17, 5);
                    g2.drawLine(7, 17, 7, 21);
                    g2.drawLine(17, 17, 17, 21);
                }
                case CODE -> {
                    // < / >
                    g2.drawLine(7, 7, 3, 12);
                    g2.drawLine(3, 12, 7, 17);
                    g2.drawLine(10, 19, 14, 5);
                    g2.drawLine(17, 7, 21, 12);
                    g2.drawLine(21, 12, 17, 17);
                }
                case HASH -> {
                    // #
                    g2.drawLine(8, 4, 7, 20);
                    g2.drawLine(16, 4, 15, 20);
                    g2.drawLine(4, 9, 20, 9);
                    g2.drawLine(4, 15, 20, 15);
                }
                default -> {
                    // Fallback: clean bullet circle with dot
                    g2.drawOval(4, 4, 16, 16);
                    g2.fillOval(10, 10, 4, 4);
                }
            }
        }
    }
}
