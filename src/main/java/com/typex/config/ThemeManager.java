package com.typex.config;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Design Token & Theme Management for TYPEX.
 * Features a bright, clean, professional desktop visual identity
 * with high-contrast typography, restrained borders, and clear hierarchy.
 */
public final class ThemeManager {
    private static boolean dark = false; // Light theme is primary & default in Phase 4
    private static Color accent = new Color(0x2563EB); // Professional Blue (#2563EB)

    private ThemeManager() {}

    public static void applyInitial() {
        apply(false, accent);
    }

    public static void apply(boolean darkTheme, Color accentColor) {
        dark = darkTheme;
        accent = accentColor != null ? accentColor : new Color(0x2563EB);

        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception ignored) {}

        // Global UI Defaults
        UIManager.put("Component.accentColor", accent);
        UIManager.put("Component.focusColor", accent);
        UIManager.put("Component.borderColor", borderColor());
        UIManager.put("Button.default.background", accent);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", borderColor());
        UIManager.put("Panel.background", bgApp());
        UIManager.put("Panel.foreground", fgPrimary());
        UIManager.put("Label.foreground", fgPrimary());

        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }

    public static boolean isDark() { return dark; }
    public static Color accent() { return accent; }
    public static Color accentHover() { return dark ? new Color(0x3B82F6) : new Color(0x1D4ED8); }
    public static Color accentLight() { return dark ? new Color(0x1E293B) : new Color(0xEFF6FF); }
    public static Color fgOnAccent() {
        if (accent.getRed() > 200 && accent.getGreen() > 180 && accent.getBlue() < 100) {
            return new Color(0x172033);
        }
        return Color.WHITE;
    }

    // Surface & Background Colors
    public static Color bgApp() { return dark ? new Color(0x0F172A) : new Color(0xF8FAFC); }
    public static Color bgCard() { return dark ? new Color(0x1E293B) : Color.WHITE; }
    public static Color bgSecondary() { return dark ? new Color(0x334155) : new Color(0xF1F5F9); }

    // Text & Content Colors
    public static Color fgPrimary() { return dark ? new Color(0xF8FAFC) : new Color(0x172033); }
    public static Color fgSecondary() { return dark ? new Color(0xCBD5E1) : new Color(0x596579); }
    public static Color fgMuted() { return dark ? new Color(0x94A3B8) : new Color(0x778195); }

    // Semantic States
    public static Color borderColor() { return dark ? new Color(0x334155) : new Color(0xE2E8F0); }
    public static Color borderHover() { return dark ? new Color(0x475569) : new Color(0xCBD5E1); }
    public static Color colorSuccess() { return new Color(0x16A34A); }
    public static Color colorWarning() { return new Color(0xD97706); }
    public static Color colorError() { return new Color(0xDC2626); }
    public static Color colorGold() { return new Color(0xF59E0B); }

    // Standard Font Family
    public static Font getTitleFont(float size) {
        return new Font("Segoe UI", Font.BOLD, Math.round(size));
    }

    public static Font getBodyFont(float size) {
        return new Font("Segoe UI", Font.PLAIN, Math.round(size));
    }

    public static Font getMonospaceFont(float size) {
        return new Font("Consolas", Font.PLAIN, Math.round(size));
    }

    public static Font getHeadingFont(float size) {
        return getTitleFont(size);
    }

    public static Font getMonoFont(float size) {
        return getMonospaceFont(size);
    }
}
