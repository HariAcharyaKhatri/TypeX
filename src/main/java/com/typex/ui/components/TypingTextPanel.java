package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.service.TypingEngine;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * High-performance, Large-Format Typing Passage Display.
 * Features 24-30px typography, multi-line auto-scrolling, clear character states,
 * and high-contrast word highlighting.
 */
public class TypingTextPanel extends JTextPane {
    private TypingEngine engine;
    private int passageFontSize = 26; // Default large reading size

    private Style styleCorrect;
    private Style styleIncorrect;
    private Style stylePending;
    private Style styleCurrentChar;
    private Style styleCurrentWord;

    private String currentTarget = "";
    private int lastRenderedCursor = -1;

    public TypingTextPanel() {
        setEditable(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setMargin(new Insets(20, 24, 20, 24));
        updatePanelFont();
        applyTheme();
    }

    public void setPassageFontSize(int size) {
        if (size >= 18 && size <= 38) {
            this.passageFontSize = size;
            updatePanelFont();
            applyTheme();
            if (engine != null) {
                fullRender();
            }
        }
    }

    public void setFontSize(int size) {
        setPassageFontSize(size);
    }

    public int getPassageFontSize() {
        return passageFontSize;
    }

    private void updatePanelFont() {
        Font font = new Font("Segoe UI", Font.PLAIN, passageFontSize);
        // Fallback check
        if (!"Segoe UI".equalsIgnoreCase(font.getFamily())) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, passageFontSize);
        }
        setFont(font);
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setForeground(ThemeManager.fgPrimary());
        setCaretColor(ThemeManager.accent());
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        StyledDocument doc = getStyledDocument();

        // 1. Correct character: Deep forest green
        styleCorrect = doc.addStyle("styleCorrect", null);
        StyleConstants.setForeground(styleCorrect, new Color(0x16A34A));
        StyleConstants.setBackground(styleCorrect, ThemeManager.bgCard());
        StyleConstants.setUnderline(styleCorrect, false);

        // 2. Incorrect character: Strong red with soft pinkish highlight
        styleIncorrect = doc.addStyle("styleIncorrect", null);
        StyleConstants.setForeground(styleIncorrect, new Color(0xDC2626));
        StyleConstants.setBackground(styleIncorrect, ThemeManager.isDark() ? new Color(0x451212) : new Color(0xFEE2E2));
        StyleConstants.setUnderline(styleIncorrect, true);

        // 3. Upcoming/Pending character: Muted readable slate
        stylePending = doc.addStyle("stylePending", null);
        StyleConstants.setForeground(stylePending, ThemeManager.fgMuted());
        StyleConstants.setBackground(stylePending, ThemeManager.bgCard());
        StyleConstants.setUnderline(stylePending, false);

        // 4. Current character to type: Primary Blue cursor underline + subtle blue box
        styleCurrentChar = doc.addStyle("styleCurrentChar", null);
        StyleConstants.setForeground(styleCurrentChar, ThemeManager.accent());
        StyleConstants.setBackground(styleCurrentChar, ThemeManager.isDark() ? new Color(0x1E3A8A) : new Color(0xDBEAFE));
        StyleConstants.setUnderline(styleCurrentChar, true);

        // 5. Current word pending letters: Dark primary text
        styleCurrentWord = doc.addStyle("styleCurrentWord", null);
        StyleConstants.setForeground(styleCurrentWord, ThemeManager.fgPrimary());
        StyleConstants.setBackground(styleCurrentWord, ThemeManager.bgCard());
        StyleConstants.setUnderline(styleCurrentWord, false);

        if (engine != null) {
            fullRender();
        }
    }

    public void setEngine(TypingEngine e) {
        this.engine = e;
        this.currentTarget = (e != null) ? e.getTarget() : "";
        this.lastRenderedCursor = -1;
        fullRender();
    }

    public void render() {
        if (engine == null) return;
        fullRender();
        scrollToCursor();
    }

    private void fullRender() {
        if (engine == null) return;
        StyledDocument doc = getStyledDocument();
        String target = engine.getTarget();
        String typed = engine.getTyped();
        int cur = engine.getCursor();

        try {
            doc.remove(0, doc.getLength());

            // Determine active word boundaries
            int wordStart = cur;
            while (wordStart > 0 && !Character.isWhitespace(target.charAt(wordStart - 1))) {
                wordStart--;
            }
            int wordEnd = cur;
            while (wordEnd < target.length() && !Character.isWhitespace(target.charAt(wordEnd))) {
                wordEnd++;
            }

            for (int i = 0; i < target.length(); i++) {
                String ch = String.valueOf(target.charAt(i));
                Style s;
                if (i < typed.length()) {
                    s = (typed.charAt(i) == target.charAt(i)) ? styleCorrect : styleIncorrect;
                } else if (i == cur) {
                    s = styleCurrentChar;
                } else if (i >= wordStart && i < wordEnd) {
                    s = styleCurrentWord;
                } else {
                    s = stylePending;
                }
                doc.insertString(doc.getLength(), ch, s);
            }

            int caretPos = Math.min(cur, doc.getLength());
            setCaretPosition(caretPos);
        } catch (BadLocationException ignored) {}
    }

    private void scrollToCursor() {
        if (engine == null) return;
        int cur = engine.getCursor();
        SwingUtilities.invokeLater(() -> {
            try {
                Rectangle2D rect = modelToView2D(cur);
                if (rect != null) {
                    // Center the line vertically in the enclosing viewport
                    Container parent = getParent();
                    if (parent instanceof JViewport vp) {
                        int viewHeight = vp.getHeight();
                        int targetY = Math.max(0, (int) rect.getY() - (viewHeight / 3));
                        vp.setViewPosition(new Point(0, targetY));
                    } else {
                        scrollRectToVisible(rect.getBounds());
                    }
                }
            } catch (Exception ignored) {}
        });
    }
}
