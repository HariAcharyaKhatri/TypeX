package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Word Rush: Fast-paced typing streak challenge.
 * Chain consecutive words before the word timer expires to build up your combo multiplier!
 */
public class WordRushGame extends BaseGamePanel {

    private static final String[] WORD_POOL = {
        "swift", "spark", "blaze", "pulse", "storm", "quick", "flash", "orbit", "drift", "frost",
        "quantum", "velocity", "champion", "keyboard", "dynamo", "rhythm", "galaxy", "thunder",
        "horizon", "matrix", "gravity", "starlight", "hyperdrive", "cybernetic", "resonance",
        "supernova", "atmosphere", "astronomy", "frequency", "millennium", "accelerate"
    };

    private final GameCanvas canvas = new GameCanvas();
    private final Random rng = new Random();

    private String targetWord = "";
    private StringBuilder typedWord = new StringBuilder();
    private int lives = 3;
    private int combo = 1;
    private int maxCombo = 1;
    private double wordTimeRemaining = 5.0;
    private double wordTimeLimit = 5.0;

    public WordRushGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.WORD_RUSH, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "COMBO";
    }

    @Override
    protected String getMetric1Value() {
        return combo + "x";
    }

    @Override
    protected String getMetric2Name() {
        return "LIVES";
    }

    @Override
    protected String getMetric2Value() {
        return "♥".repeat(Math.max(0, lives));
    }

    @Override
    protected void onGameStart() {
        lives = 3;
        combo = 1;
        maxCombo = 1;
        spawnNextWord();
    }

    private void spawnNextWord() {
        targetWord = WORD_POOL[rng.nextInt(WORD_POOL.length)];
        typedWord.setLength(0);
        wordTimeLimit = Math.max(2.2, 5.0 - (level * 0.25));
        wordTimeRemaining = wordTimeLimit;
    }

    @Override
    protected void onGameTick() {
        wordTimeRemaining -= 0.016;
        if (wordTimeRemaining <= 0) {
            // Word expired
            lives--;
            combo = 1;
            SoundManager.error();
            if (lives <= 0) {
                endGame();
            } else {
                spawnNextWord();
            }
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;
        int nextIdx = typedWord.length();

        if (nextIdx < targetWord.length()) {
            char expected = targetWord.charAt(nextIdx);
            if (c == expected) {
                typedWord.append(c);
                correctKeystrokes++;
                SoundManager.keypress();

                // Check if word complete
                if (typedWord.length() == targetWord.length()) {
                    wordsCompleted++;
                    score += targetWord.length() * 10 * combo;
                    combo = Math.min(5, combo + 1);
                    maxCombo = Math.max(maxCombo, combo);
                    if (wordsCompleted % 8 == 0) {
                        level++;
                    }
                    SoundManager.complete();
                    spawnNextWord();
                }
            } else {
                errors++;
                combo = 1;
                SoundManager.error();
            }
        }
    }

    @Override
    protected void handleBackspace() {
        if (typedWord.length() > 0) {
            typedWord.setLength(typedWord.length() - 1);
            totalKeystrokes++;
        }
    }

    @Override
    protected void handleSpace() {
        // Space acts as word submission attempt or skip if complete
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

            // Word Card Box
            int cardW = Math.min(520, w - 80);
            int cardH = 240;
            int cardX = (w - cardW) / 2;
            int cardY = (h - cardH) / 2 - 20;

            g2.setColor(ThemeManager.bgCard());
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
            g2.setColor(combo > 1 ? ThemeManager.accent() : ThemeManager.borderColor());
            g2.setStroke(new BasicStroke(combo > 1 ? 2.5f : 1.5f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

            // Time bar inside card
            int barH = 6;
            int barY = cardY + 12;
            int barW = cardW - 40;
            int barX = cardX + 20;

            g2.setColor(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xE2E8F0));
            g2.fillRoundRect(barX, barY, barW, barH, 4, 4);

            double frac = Math.max(0, Math.min(1.0, wordTimeRemaining / wordTimeLimit));
            int fillW = (int) (barW * frac);
            g2.setColor(frac < 0.25 ? ThemeManager.colorError() : (frac < 0.5 ? ThemeManager.colorWarning() : ThemeManager.accent()));
            g2.fillRoundRect(barX, barY, fillW, barH, 4, 4);

            // Combo multiplier badge
            if (combo > 1) {
                g2.setFont(ThemeManager.getHeadingFont(14f));
                g2.setColor(ThemeManager.colorWarning());
                String comboStr = combo + "x COMBO MULTIPLIER!";
                FontMetrics cm = g2.getFontMetrics();
                g2.drawString(comboStr, (w - cm.stringWidth(comboStr)) / 2, cardY + 44);
            }

            // Word display
            Font wordFont = ThemeManager.getMonoFont(36f);
            g2.setFont(wordFont);
            FontMetrics fm = g2.getFontMetrics();

            int textY = cardY + 130;
            int fullWidth = fm.stringWidth(targetWord);
            int startX = (w - fullWidth) / 2;

            for (int i = 0; i < targetWord.length(); i++) {
                char ch = targetWord.charAt(i);
                String chStr = String.valueOf(ch);
                int charW = fm.stringWidth(chStr);

                if (i < typedWord.length()) {
                    g2.setColor(ThemeManager.colorSuccess());
                } else if (i == typedWord.length()) {
                    g2.setColor(ThemeManager.accent());
                    // Underline active char
                    g2.fillRect(startX, textY + 6, charW, 3);
                } else {
                    g2.setColor(ThemeManager.fgMuted());
                }

                g2.drawString(chStr, startX, textY);
                startX += charW;
            }

            // Sub-instruction
            g2.setFont(ThemeManager.getBodyFont(13f));
            g2.setColor(ThemeManager.fgMuted());
            String tip = "Type the word accurately before the gauge empties!";
            FontMetrics tm = g2.getFontMetrics();
            g2.drawString(tip, (w - tm.stringWidth(tip)) / 2, cardY + 200);

            g2.dispose();
        }
    }
}
