package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.service.TypingEngine;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class TypingTextPanel extends JTextPane {
    private TypingEngine engine;
    private Style correct, incorrect, pending, current;

    public TypingTextPanel() {
        setEditable(false);
        setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        if (!"JetBrains Mono".equals(getFont().getName())) {
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        }
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        applyTheme();
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setForeground(ThemeManager.fgPrimary());
        setCaretColor(ThemeManager.accent());

        StyledDocument doc = getStyledDocument();
        correct = doc.addStyle("c", null);
        StyleConstants.setForeground(correct, new Color(0x22C55E));

        incorrect = doc.addStyle("i", null);
        StyleConstants.setForeground(incorrect, new Color(0xEF4444));
        StyleConstants.setBackground(incorrect, ThemeManager.isDark() ? new Color(0x3B1212) : new Color(0xFEE2E2));

        pending = doc.addStyle("p", null);
        StyleConstants.setForeground(pending, ThemeManager.fgMuted());

        current = doc.addStyle("cur", null);
        StyleConstants.setUnderline(current, true);
        StyleConstants.setForeground(current, ThemeManager.accent());
        StyleConstants.setBackground(current, ThemeManager.isDark() ? new Color(0x2A2A1A) : new Color(0xFEF3C7));

        render();
    }

    public void setEngine(TypingEngine e) {
        this.engine = e;
        render();
    }

    public void render() {
        if (engine == null) return;
        StyledDocument doc = getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            String target = engine.getTarget();
            String typed = engine.getTyped();
            for (int i = 0; i < target.length(); i++) {
                String ch = String.valueOf(target.charAt(i));
                Style s;
                if (i < typed.length()) {
                    s = (typed.charAt(i) == target.charAt(i)) ? correct : incorrect;
                } else if (i == typed.length()) {
                    s = current;
                } else {
                    s = pending;
                }
                doc.insertString(doc.getLength(), ch, s);
            }
            int caretPos = Math.min(engine.getCursor(), doc.getLength());
            setCaretPosition(caretPos);
        } catch (BadLocationException ignored) {}
    }
}
