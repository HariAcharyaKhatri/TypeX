package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Typing Survival: Vitality continuously drains.
 * Correct keystrokes restore life energy; typos drain remaining seconds!
 */
public class TypingSurvivalGame extends BaseGamePanel {

    private static final String[] SURVIVAL_WORDS = {
        "breathe", "endure", "resilient", "shield", "pulse", "stamina", "vitality", "oxygen",
        "reactor", "battery", "circuits", "beacon", "emergency", "override", "pressure",
        "thrusters", "stabilize", "telemetry", "generator", "catalyst", "hyperdrive",
        "safeguard", "velocity", "deflector", "protocol", "containment", "filtration"
    };

    private final GameCanvas canvas = new GameCanvas();
    private final Random rng = new Random();

    private double vitalitySeconds = 30.0;
    private final double maxVitality = 50.0;
    private String currentWord = "";
    private StringBuilder typed = new StringBuilder();

    public TypingSurvivalGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.TYPING_SURVIVAL, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "VITALITY";
    }

    @Override
    protected String getMetric1Value() {
        return String.format("%.1fs", Math.max(0, vitalitySeconds));
    }

    @Override
    protected String getMetric2Name() {
        return "SURVIVED";
    }

    @Override
    protected String getMetric2Value() {
        return elapsedSeconds + "s";
    }

    @Override
    protected void onGameStart() {
        vitalitySeconds = 30.0;
        spawnNextWord();
    }

    private void spawnNextWord() {
        currentWord = SURVIVAL_WORDS[rng.nextInt(SURVIVAL_WORDS.length)];
        typed.setLength(0);
    }

    @Override
    protected void onGameTick() {
        double drainPerTick = 0.016 * (1.0 + (level * 0.05));
        vitalitySeconds -= drainPerTick;

        if (vitalitySeconds <= 0) {
            vitalitySeconds = 0;
            endGame();
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;
        int idx = typed.length();

        if (idx < currentWord.length()) {
            char expected = currentWord.charAt(idx);
            if (c == expected) {
                typed.append(c);
                correctKeystrokes++;
                SoundManager.keypress();

                if (typed.length() == currentWord.length()) {
                    wordsCompleted++;
                    score += currentWord.length() * 20;
                    vitalitySeconds = Math.min(maxVitality, vitalitySeconds + 3.5);
                    SoundManager.complete();

                    if (wordsCompleted % 6 == 0) {
                        level++;
                    }
                    spawnNextWord();
                }
            } else {
                errors++;
                vitalitySeconds = Math.max(0, vitalitySeconds - 2.0);
                SoundManager.error();
                if (vitalitySeconds <= 0) {
                    endGame();
                }
            }
        }
    }

    @Override
    protected void handleBackspace() {
        if (typed.length() > 0) {
            typed.setLength(typed.length() - 1);
            totalKeystrokes++;
        }
    }

    @Override
    protected void handleSpace() {}

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

            // Big Vitality Gauge
            int gaugeW = Math.min(600, w - 80);
            int gaugeH = 24;
            int gaugeX = (w - gaugeW) / 2;
            int gaugeY = (h / 2) - 100;

            g2.setColor(ThemeManager.isDark() ? new Color(0x2A2A2A) : new Color(0xE2E8F0));
            g2.fillRoundRect(gaugeX, gaugeY, gaugeW, gaugeH, 12, 12);

            double frac = Math.max(0, Math.min(1.0, vitalitySeconds / maxVitality));
            int fillW = (int) (gaugeW * frac);

            Color gaugeColor = (vitalitySeconds < 8.0) ? ThemeManager.colorError()
                    : (vitalitySeconds < 18.0 ? ThemeManager.colorWarning() : ThemeManager.colorSuccess());
            g2.setColor(gaugeColor);
            g2.fillRoundRect(gaugeX, gaugeY, fillW, gaugeH, 12, 12);

            g2.setColor(ThemeManager.borderColor());
            g2.drawRoundRect(gaugeX, gaugeY, gaugeW, gaugeH, 12, 12);

            // Vitality label
            g2.setFont(ThemeManager.getMonoFont(12f));
            g2.setColor(ThemeManager.fgPrimary());
            String vStr = String.format("OXYGEN VITALITY: %.1fs / %.0fs", vitalitySeconds, maxVitality);
            FontMetrics vm = g2.getFontMetrics();
            g2.drawString(vStr, (w - vm.stringWidth(vStr)) / 2, gaugeY - 10);

            // Center Word Card
            int cardW = Math.min(520, w - 80);
            int cardH = 160;
            int cardX = (w - cardW) / 2;
            int cardY = gaugeY + 50;

            g2.setColor(ThemeManager.bgCard());
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2.setColor(ThemeManager.borderColor());
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);

            // Word display
            Font wordFont = ThemeManager.getMonoFont(34f);
            g2.setFont(wordFont);
            FontMetrics fm = g2.getFontMetrics();
            int textY = cardY + 95;
            int fullW = fm.stringWidth(currentWord);
            int startX = (w - fullW) / 2;

            for (int i = 0; i < currentWord.length(); i++) {
                char ch = currentWord.charAt(i);
                String chStr = String.valueOf(ch);
                int chW = fm.stringWidth(chStr);

                if (i < typed.length()) {
                    g2.setColor(ThemeManager.colorSuccess());
                } else if (i == typed.length()) {
                    g2.setColor(ThemeManager.accent());
                    g2.fillRect(startX, textY + 6, chW, 3);
                } else {
                    g2.setColor(ThemeManager.fgMuted());
                }

                g2.drawString(chStr, startX, textY);
                startX += chW;
            }

            g2.dispose();
        }
    }
}
