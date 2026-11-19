package com.typex.config;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public final class ThemeManager {
    private static boolean dark = true;
    private static Color accent = new Color(0xFFC107);

    private ThemeManager() {}

    public static void applyInitial() {
        apply(true, accent);
    }

    public static void apply(boolean darkTheme, Color accentColor) {
        dark = darkTheme;
        accent = accentColor;

        try {
            if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        } catch (Exception ignored) {}

        UIManager.put("Component.accentColor", accent);
        UIManager.put("Component.focusColor", accent);
        UIManager.put("Button.default.background", accent);
        UIManager.put("Button.default.foreground", Color.BLACK);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 12);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Panel.background", dark ? new Color(0x1A1A1A) : new Color(0xF4F4F6));
        UIManager.put("Panel.foreground", dark ? Color.WHITE : Color.BLACK);

        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }

    public static boolean isDark() { return dark; }
    public static Color accent() { return accent; }
    public static Color bgCard() { return dark ? new Color(0x232323) : Color.WHITE; }
    public static Color bgApp() { return dark ? new Color(0x141414) : new Color(0xF4F4F6); }
    public static Color fgPrimary() { return dark ? new Color(0xEFEFEF) : new Color(0x1A1A1A); }
    public static Color fgMuted() { return dark ? new Color(0x9AA0A6) : new Color(0x6B7280); }
}
