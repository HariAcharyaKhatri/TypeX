package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Falling Words: Words cascade down the matrix.
 * Type them before they collide with your defense baseline!
 */
public class FallingWordsGame extends BaseGamePanel {

    private static final String[] WORD_DICTIONARY = {
        "alpha", "bravo", "cloud", "delta", "eagle", "focus", "globe", "hover", "irony", "joker",
        "laser", "magic", "nexus", "orbit", "prism", "quest", "radar", "solar", "titan", "ultra",
        "vapor", "xenon", "yield", "zenith", "beacon", "crypto", "engine", "fusion", "matrix", "vector",
        "quantum", "nebula", "velocity", "gravity", "horizon", "pulsar", "astral", "dynamic"
    };

    private static class FallingWord {
        String text;
        double x;
        double y;
        double speed;
        int typedLen = 0;

        FallingWord(String text, double x, double y, double speed) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    private final GameCanvas canvas = new GameCanvas();
    private final List<FallingWord> activeWords = new ArrayList<>();
    private final Random rng = new Random();

    private FallingWord lockedWord = null;
    private int lives = 5;
    private int spawnCounter = 0;
    private int spawnInterval = 90; // frames (~1.5s)

    public FallingWordsGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.FALLING_WORDS, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "LEVEL";
    }

    @Override
    protected String getMetric1Value() {
        return String.valueOf(level);
    }

    @Override
    protected String getMetric2Name() {
        return "SHIELDS";
    }

    @Override
    protected String getMetric2Value() {
        return "♥".repeat(Math.max(0, lives));
    }

    @Override
    protected void onGameStart() {
        lives = 5;
        activeWords.clear();
        lockedWord = null;
        spawnInterval = 90;
        spawnCounter = 0;
        spawnWord();
        spawnWord();
    }

    private void spawnWord() {
        int canvasW = Math.max(300, canvas.getWidth());
        String word = WORD_DICTIONARY[rng.nextInt(WORD_DICTIONARY.length)];
        double x = 40 + rng.nextInt(Math.max(100, canvasW - 160));
        double baseSpeed = 0.8 + (level * 0.15);
        double speed = baseSpeed + (rng.nextDouble() * 0.4);
        activeWords.add(new FallingWord(word, x, 30, speed));
    }

    @Override
    protected void onGameTick() {
        spawnCounter++;
        if (spawnCounter >= spawnInterval) {
            spawnCounter = 0;
            if (activeWords.size() < 6) {
                spawnWord();
            }
        }

        int baselineY = Math.max(100, canvas.getHeight() - 60);

        Iterator<FallingWord> it = activeWords.iterator();
        while (it.hasNext()) {
            FallingWord fw = it.next();
            fw.y += fw.speed;

            if (fw.y >= baselineY) {
                // Word hit bottom line!
                lives--;
                SoundManager.error();
                if (lockedWord == fw) {
                    lockedWord = null;
                }
                it.remove();

                if (lives <= 0) {
                    endGame();
                    return;
                }
            }
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;

        if (lockedWord != null) {
            if (lockedWord.typedLen < lockedWord.text.length()) {
                char expected = lockedWord.text.charAt(lockedWord.typedLen);
                if (c == expected) {
                    lockedWord.typedLen++;
                    correctKeystrokes++;
                    SoundManager.keypress();

                    if (lockedWord.typedLen == lockedWord.text.length()) {
                        wordsCompleted++;
                        score += lockedWord.text.length() * 15;
                        activeWords.remove(lockedWord);
                        lockedWord = null;
                        SoundManager.complete();

                        if (wordsCompleted % 8 == 0) {
                            level++;
                            spawnInterval = Math.max(45, 90 - (level * 5));
                        }
                    }
                } else {
                    errors++;
                    SoundManager.error();
                }
            }
            return;
        }

        // Try to match any falling word's first character (prioritizing the lowest one)
        FallingWord bestMatch = null;
        for (FallingWord fw : activeWords) {
            if (fw.text.length() > 0 && fw.text.charAt(0) == c) {
                if (bestMatch == null || fw.y > bestMatch.y) {
                    bestMatch = fw;
                }
            }
        }

        if (bestMatch != null) {
            lockedWord = bestMatch;
            lockedWord.typedLen = 1;
            correctKeystrokes++;
            SoundManager.keypress();
            if (lockedWord.text.length() == 1) {
                wordsCompleted++;
                score += 15;
                activeWords.remove(lockedWord);
                lockedWord = null;
                SoundManager.complete();
            }
        } else {
            errors++;
            SoundManager.error();
        }
    }

    @Override
    protected void handleBackspace() {
        if (lockedWord != null && lockedWord.typedLen > 0) {
            lockedWord.typedLen--;
            if (lockedWord.typedLen == 0) {
                lockedWord = null;
            }
            totalKeystrokes++;
        }
    }

    @Override
    protected void handleSpace() {
        // Space drops current word lock
        if (lockedWord != null) {
            lockedWord.typedLen = 0;
            lockedWord = null;
        }
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
            int baselineY = h - 60;

            // Draw baseline danger line
            g2.setColor(ThemeManager.colorError());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8f, 6f}, 0));
            g2.drawLine(20, baselineY, w - 20, baselineY);

            g2.setFont(ThemeManager.getMonoFont(11f));
            g2.drawString("DEFENSE PERIMETER", 24, baselineY - 6);

            // Draw falling words
            Font wordFont = ThemeManager.getMonoFont(18f);
            g2.setFont(wordFont);
            FontMetrics fm = g2.getFontMetrics();

            for (FallingWord fw : activeWords) {
                int fwWidth = fm.stringWidth(fw.text);
                int padX = 12;
                int padY = 6;
                int boxX = (int) fw.x - padX;
                int boxY = (int) fw.y - fm.getAscent() - padY;
                int boxW = fwWidth + (padX * 2);
                int boxH = fm.getHeight() + (padY * 2);

                boolean isLocked = (fw == lockedWord);

                // Word background pill
                g2.setColor(isLocked ? (ThemeManager.isDark() ? new Color(0x1E3A8A) : new Color(0xDBEAFE))
                                     : ThemeManager.bgCard());
                g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
                g2.setColor(isLocked ? ThemeManager.accent() : ThemeManager.borderColor());
                g2.setStroke(new BasicStroke(isLocked ? 2f : 1f));
                g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

                // Word text
                int curX = (int) fw.x;
                for (int i = 0; i < fw.text.length(); i++) {
                    char ch = fw.text.charAt(i);
                    String chStr = String.valueOf(ch);
                    int chW = fm.stringWidth(chStr);

                    if (isLocked && i < fw.typedLen) {
                        g2.setColor(ThemeManager.colorSuccess());
                    } else if (isLocked && i == fw.typedLen) {
                        g2.setColor(ThemeManager.accent());
                    } else {
                        g2.setColor(ThemeManager.fgPrimary());
                    }

                    g2.drawString(chStr, curX, (int) fw.y);
                    curX += chW;
                }
            }

            g2.dispose();
        }
    }
}
