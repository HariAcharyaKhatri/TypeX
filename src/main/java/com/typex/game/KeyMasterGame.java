package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Key Master: Reflex and finger-placement reaction trainer.
 */
public class KeyMasterGame extends BaseGamePanel {

    private final GameCanvas canvas = new GameCanvas();
    private final Random rng = new Random();
    private final String keyPool = "abcdefghijklmnopqrstuvwxyz;,./";

    private char targetKey = 'a';
    private long keySpawnNanos = 0;
    private int trialsRemaining = 30;
    private final int totalTrials = 30;
    private final List<Double> reactionTimes = new ArrayList<>();
    private String lastFeedback = "Press the designated key!";
    private Color feedbackColor = ThemeManager.accent();

    public KeyMasterGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.KEY_MASTER, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "REMAINING";
    }

    @Override
    protected String getMetric1Value() {
        return trialsRemaining + " / " + totalTrials;
    }

    @Override
    protected String getMetric2Name() {
        return "AVG REACTION";
    }

    @Override
    protected String getMetric2Value() {
        if (reactionTimes.isEmpty()) return "-";
        double avg = reactionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return String.format("%.0f ms", avg);
    }

    @Override
    protected void onGameStart() {
        trialsRemaining = totalTrials;
        reactionTimes.clear();
        lastFeedback = "Get Ready!";
        feedbackColor = ThemeManager.accent();
        spawnNextKey();
    }

    private void spawnNextKey() {
        targetKey = keyPool.charAt(rng.nextInt(keyPool.length()));
        keySpawnNanos = System.nanoTime();
    }

    @Override
    protected void onGameTick() {}

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;
        double reactionMs = (System.nanoTime() - keySpawnNanos) / 1_000_000.0;

        if (Character.toLowerCase(c) == Character.toLowerCase(targetKey)) {
            correctKeystrokes++;
            wordsCompleted++;
            reactionTimes.add(reactionMs);
            SoundManager.keypress();

            if (reactionMs < 250) {
                score += 100;
                lastFeedback = String.format("LIGHTNING! %.0f ms (+100)", reactionMs);
                feedbackColor = ThemeManager.colorSuccess();
            } else if (reactionMs < 400) {
                score += 70;
                lastFeedback = String.format("EXCELLENT! %.0f ms (+70)", reactionMs);
                feedbackColor = ThemeManager.accent();
            } else if (reactionMs < 600) {
                score += 40;
                lastFeedback = String.format("GOOD! %.0f ms (+40)", reactionMs);
                feedbackColor = ThemeManager.colorWarning();
            } else {
                score += 20;
                lastFeedback = String.format("SLOW: %.0f ms (+20)", reactionMs);
                feedbackColor = ThemeManager.fgMuted();
            }

            trialsRemaining--;
            if (trialsRemaining <= 0) {
                SoundManager.complete();
                endGame();
            } else {
                spawnNextKey();
            }
        } else {
            errors++;
            SoundManager.error();
            lastFeedback = "WRONG KEY! -10";
            score = Math.max(0, score - 10);
            feedbackColor = ThemeManager.colorError();
        }
    }

    @Override
    protected void handleBackspace() {}

    @Override
    protected void handleSpace() {}

    private String getFingerGuidance(char ch) {
        com.typex.util.KeyboardLayout.LayoutInfo info = com.typex.util.KeyboardLayout.getLayout("US QWERTY");
        com.typex.util.KeyboardLayout.Finger f = info.getFingerForChar(ch);
        return f != null ? f.displayName : "Index Finger";
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

            // Target Key Big Card
            int cardW = Math.min(450, w - 80);
            int cardH = 260;
            int cardX = (w - cardW) / 2;
            int cardY = (h - cardH) / 2 - 20;

            g2.setColor(ThemeManager.bgCard());
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
            g2.setColor(ThemeManager.borderColor());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

            // Keycap button representation
            int keyW = 100;
            int keyH = 100;
            int keyX = (w - keyW) / 2;
            int keyY = cardY + 30;

            g2.setColor(ThemeManager.isDark() ? new Color(0x242424) : new Color(0xF1F5F9));
            g2.fillRoundRect(keyX, keyY, keyW, keyH, 16, 16);
            g2.setColor(ThemeManager.accent());
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(keyX, keyY, keyW, keyH, 16, 16);

            // Target character inside keycap
            g2.setFont(ThemeManager.getMonoFont(46f));
            g2.setColor(ThemeManager.fgPrimary());
            String keyStr = String.valueOf(Character.toUpperCase(targetKey));
            FontMetrics km = g2.getFontMetrics();
            g2.drawString(keyStr, keyX + (keyW - km.stringWidth(keyStr)) / 2, keyY + 68);

            // Finger guide instruction
            g2.setFont(ThemeManager.getHeadingFont(15f));
            g2.setColor(ThemeManager.accent());
            String finger = "FINGER: " + getFingerGuidance(targetKey).toUpperCase();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(finger, (w - fm.stringWidth(finger)) / 2, keyY + 140);

            // Feedback banner
            g2.setFont(ThemeManager.getMonoFont(14f));
            g2.setColor(feedbackColor);
            FontMetrics fbm = g2.getFontMetrics();
            g2.drawString(lastFeedback, (w - fbm.stringWidth(lastFeedback)) / 2, keyY + 175);

            g2.dispose();
        }
    }
}
