package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Code Runner: Programmer's syntax challenge.
 * Tests agility on brackets, symbols, syntax patterns, and developer keywords.
 */
public class CodeRunnerGame extends BaseGamePanel {

    private static final String[][] CODE_SNIPPETS = {
        {"Java", "public static <T> List<T> filter(List<T> list, Predicate<T> p) {\n    return list.stream().filter(p).toList();\n}"},
        {"JavaScript", "const fetchUser = async (id) => {\n    const res = await api.get(`/users/${id}`);\n    return res.data;\n};"},
        {"Python", "def quick_sort(arr: list[int]) -> list[int]:\n    if len(arr) <= 1: return arr\n    pivot = arr[len(arr) // 2]\n    return [x for x in arr if x < pivot] + [pivot] + [x for x in arr if x > pivot]"},
        {"SQL", "SELECT u.id, u.username, COUNT(r.id) AS total\nFROM users u\nJOIN results r ON u.id = r.user_id\nWHERE r.accuracy >= 95.0\nGROUP BY u.id;"},
        {"HTML/CSS", "<div class=\"code-card active\" data-lang=\"java\">\n    <span class=\"keyword\">return</span>\n</div>"}
    };

    private final GameCanvas canvas = new GameCanvas();
    private final Random rng = new Random();

    private String currentLanguage = "Java";
    private String currentSnippet = "";
    private StringBuilder typedSnippet = new StringBuilder();
    private int snippetIndex = 0;
    private double timeRemaining = 60.0;

    public CodeRunnerGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.CODE_RUNNER, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "LANGUAGE";
    }

    @Override
    protected String getMetric1Value() {
        return currentLanguage;
    }

    @Override
    protected String getMetric2Name() {
        return "TIME LEFT";
    }

    @Override
    protected String getMetric2Value() {
        return String.format("%.0fs", Math.max(0, timeRemaining));
    }

    @Override
    protected void onGameStart() {
        snippetIndex = 0;
        timeRemaining = 60.0;
        loadSnippet();
    }

    private void loadSnippet() {
        String[] item = CODE_SNIPPETS[snippetIndex % CODE_SNIPPETS.length];
        currentLanguage = item[0];
        currentSnippet = item[1];
        typedSnippet.setLength(0);
    }

    @Override
    protected void onGameTick() {
        timeRemaining -= 0.016;
        if (timeRemaining <= 0) {
            timeRemaining = 0;
            endGame();
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;
        int idx = typedSnippet.length();

        if (idx < currentSnippet.length()) {
            char expected = currentSnippet.charAt(idx);

            // Handle newline vs enter/return
            if (expected == '\n' && (c == '\n' || c == '\r')) {
                c = '\n';
            }

            if (c == expected) {
                typedSnippet.append(c);
                correctKeystrokes++;
                SoundManager.keypress();

                if (typedSnippet.length() == currentSnippet.length()) {
                    wordsCompleted += currentSnippet.split("\\s+").length;
                    score += 350;
                    timeRemaining = Math.min(90.0, timeRemaining + 15.0);
                    SoundManager.complete();
                    snippetIndex++;
                    level = snippetIndex + 1;
                    loadSnippet();
                }
            } else {
                errors++;
                SoundManager.error();
            }
        }
    }

    @Override
    protected void handleBackspace() {
        if (typedSnippet.length() > 0) {
            typedSnippet.setLength(typedSnippet.length() - 1);
            totalKeystrokes++;
        }
    }

    @Override
    protected void handleSpace() {
        handleTypedChar(' ');
    }

    private class GameCanvas extends JPanel {
        public GameCanvas() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            int cardW = Math.min(800, w - 60);
            int cardH = Math.min(380, h - 60);
            int cardX = (w - cardW) / 2;
            int cardY = (h - cardH) / 2;

            // Editor background card
            g2.setColor(ThemeManager.isDark() ? new Color(0x1E1E1E) : new Color(0xF8FAFC));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2.setColor(ThemeManager.borderColor());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);

            // Editor header tab
            g2.setColor(ThemeManager.isDark() ? new Color(0x252526) : new Color(0xEDF2F7));
            g2.fillRoundRect(cardX, cardY, cardW, 36, 16, 16);
            g2.fillRect(cardX, cardY + 20, cardW, 16);

            g2.setFont(ThemeManager.getMonoFont(12f));
            g2.setColor(ThemeManager.accent());
            g2.drawString("SOURCE: " + currentLanguage.toUpperCase(), cardX + 16, cardY + 23);

            // Snippet text rendering
            Font codeFont = ThemeManager.getMonoFont(16f);
            g2.setFont(codeFont);
            FontMetrics fm = g2.getFontMetrics();

            int lineHeight = fm.getHeight() + 4;
            int startX = cardX + 24;
            int currentY = cardY + 68;

            String[] lines = currentSnippet.split("\n", -1);
            int charCursor = 0;

            for (String line : lines) {
                int lineX = startX;
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    String chStr = String.valueOf(ch);
                    int chW = fm.stringWidth(chStr);

                    if (charCursor < typedSnippet.length()) {
                        g2.setColor(ThemeManager.colorSuccess());
                    } else if (charCursor == typedSnippet.length()) {
                        g2.setColor(ThemeManager.accent());
                        g2.fillRect(lineX, currentY + 3, Math.max(8, chW), 2);
                    } else {
                        g2.setColor(ThemeManager.fgPrimary());
                    }

                    g2.drawString(chStr, lineX, currentY);
                    lineX += chW;
                    charCursor++;
                }

                // Account for \n in character stream
                charCursor++;
                currentY += lineHeight;
            }

            g2.dispose();
        }
    }
}
