package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Sentence Sprint: Drag-race speed typing challenge against a dynamic pace runner!
 */
public class SentenceSprintGame extends BaseGamePanel {

    private static final String[] SPRINT_SENTENCES = {
        "The quick brown fox jumps gracefully over the lazy dog.",
        "Precision beats speed, but precision with speed creates mastery.",
        "Every keystroke is a stepping stone to effortless typing speed.",
        "Focus on smooth rhythm and let muscle memory guide your fingers.",
        "Speed comes naturally when every letter lands on its exact target.",
        "Breathe calmly and maintain steady cadence through every single word.",
        "Discipline transforms practice into high performance capability."
    };

    private final GameCanvas canvas = new GameCanvas();
    private final Random rng = new Random();

    private String targetSentence = "";
    private StringBuilder typedSentence = new StringBuilder();
    private int currentRound = 1;
    private final int totalRounds = 3;

    private double paceCarProgress = 0.0; // 0 to 1
    private double targetWpm = 50.0;
    private long roundStartNanos = 0;

    public SentenceSprintGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.SENTENCE_SPRINT, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "ROUND";
    }

    @Override
    protected String getMetric1Value() {
        return currentRound + " / " + totalRounds;
    }

    @Override
    protected String getMetric2Name() {
        return "PACE CAR";
    }

    @Override
    protected String getMetric2Value() {
        return String.format("%.0f WPM", targetWpm);
    }

    @Override
    protected void onGameStart() {
        currentRound = 1;
        targetWpm = 50.0;
        startRound();
    }

    private void startRound() {
        targetSentence = SPRINT_SENTENCES[rng.nextInt(SPRINT_SENTENCES.length)];
        typedSentence.setLength(0);
        paceCarProgress = 0.0;
        roundStartNanos = System.nanoTime();
        targetWpm = 45.0 + (currentRound * 15.0); // Round 1: 60, Round 2: 75, Round 3: 90
    }

    @Override
    protected void onGameTick() {
        if (targetSentence.isEmpty()) return;

        double roundElapsedSeconds = (System.nanoTime() - roundStartNanos) / 1_000_000_000.0;
        // Characters expected per second by pace car
        double paceCharsPerSec = (targetWpm * 5.0) / 60.0;
        paceCarProgress = Math.min(1.0, (roundElapsedSeconds * paceCharsPerSec) / targetSentence.length());

        if (paceCarProgress >= 1.0) {
            // Pace car reached finish first!
            if (typedSentence.length() < targetSentence.length()) {
                SoundManager.error();
                advanceRound(false);
            }
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;
        int idx = typedSentence.length();

        if (idx < targetSentence.length()) {
            char expected = targetSentence.charAt(idx);
            if (c == expected) {
                typedSentence.append(c);
                correctKeystrokes++;
                SoundManager.keypress();

                if (typedSentence.length() == targetSentence.length()) {
                    // Player reached finish line!
                    SoundManager.complete();
                    boolean beatPace = paceCarProgress < 1.0;
                    score += beatPace ? 400 : 200;
                    advanceRound(beatPace);
                }
            } else {
                errors++;
                SoundManager.error();
            }
        }
    }

    private void advanceRound(boolean wonRound) {
        wordsCompleted += targetSentence.split("\\s+").length;
        if (currentRound >= totalRounds) {
            endGame();
        } else {
            currentRound++;
            level = currentRound;
            startRound();
        }
    }

    @Override
    protected void handleBackspace() {
        if (typedSentence.length() > 0) {
            typedSentence.setLength(typedSentence.length() - 1);
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

            // Racetrack bounds
            int trackW = Math.min(750, w - 80);
            int trackX = (w - trackW) / 2;
            int trackY = 40;
            int laneH = 46;

            // Draw track background
            g2.setColor(ThemeManager.bgCard());
            g2.fillRoundRect(trackX, trackY, trackW, laneH * 2 + 10, 16, 16);
            g2.setColor(ThemeManager.borderColor());
            g2.drawRoundRect(trackX, trackY, trackW, laneH * 2 + 10, 16, 16);

            // Divider between lanes
            g2.setColor(ThemeManager.borderColor());
            g2.drawLine(trackX, trackY + laneH + 5, trackX + trackW, trackY + laneH + 5);

            // Finish line
            int finishX = trackX + trackW - 40;
            g2.setColor(ThemeManager.colorSuccess());
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
            g2.drawLine(finishX, trackY, finishX, trackY + laneH * 2 + 10);

            // Lane 1: Player runner
            double playerProgress = targetSentence.length() > 0
                    ? (double) typedSentence.length() / targetSentence.length() : 0.0;
            int playerRunnerX = trackX + 16 + (int) ((finishX - trackX - 24) * playerProgress);
            g2.setFont(ThemeManager.getHeadingFont(12f));
            g2.setColor(ThemeManager.accent());
            g2.drawString("YOU", trackX + 8, trackY + 28);
            g2.fillOval(playerRunnerX, trackY + 12, 22, 22);

            // Lane 2: Pace Car
            int paceRunnerX = trackX + 16 + (int) ((finishX - trackX - 24) * paceCarProgress);
            g2.setColor(ThemeManager.colorWarning());
            g2.drawString(String.format("PACE (%.0f)", targetWpm), trackX + 8, trackY + laneH + 32);
            g2.fillOval(paceRunnerX, trackY + laneH + 16, 22, 22);

            // Center Sentence Card
            int cardW = Math.min(750, w - 80);
            int cardH = 180;
            int cardX = (w - cardW) / 2;
            int cardY = trackY + laneH * 2 + 35;

            g2.setColor(ThemeManager.bgCard());
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2.setColor(ThemeManager.borderColor());
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);

            // Sentence display
            Font sentenceFont = ThemeManager.getMonoFont(22f);
            g2.setFont(sentenceFont);
            FontMetrics fm = g2.getFontMetrics();

            int textY = cardY + 95;
            int fullW = fm.stringWidth(targetSentence);
            int startX = cardX + 24;

            for (int i = 0; i < targetSentence.length(); i++) {
                char ch = targetSentence.charAt(i);
                String chStr = String.valueOf(ch);
                int chW = fm.stringWidth(chStr);

                if (i < typedSentence.length()) {
                    g2.setColor(ThemeManager.colorSuccess());
                } else if (i == typedSentence.length()) {
                    g2.setColor(ThemeManager.accent());
                    g2.fillRect(startX, textY + 4, Math.max(8, chW), 3);
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
