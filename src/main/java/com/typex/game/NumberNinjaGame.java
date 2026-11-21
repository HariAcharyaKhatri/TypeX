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
 * Number Ninja: High-precision numeric, currency, math equation, and symbol typing.
 */
public class NumberNinjaGame extends BaseGamePanel {

    private static final String[] TARGET_ITEMS = {
        "$1,250.00", "99.99%", "2026-11-21", "#42-A", "3x + 12 = 45",
        "192.168.1.104", "$49.99", "7.5% APR", "f(x) = x^2 - 4", "€850.50",
        "+1(800)555-0199", "12/31/2025", "[v2.1.0]", "100 * 4 = 400", "0xFF00AA"
    };

    private static class NinjaTarget {
        String text;
        double x, y;
        double vx, vy;
        int typedLen = 0;
        double life = 8.0; // seconds before vanishing

        NinjaTarget(String text, double x, double y, double vx, double vy) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    private final GameCanvas canvas = new GameCanvas();
    private final List<NinjaTarget> targets = new ArrayList<>();
    private final Random rng = new Random();

    private NinjaTarget activeTarget = null;
    private int lives = 3;
    private int combo = 1;
    private int spawnCounter = 0;

    public NumberNinjaGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.NUMBER_NINJA, onExitToCatalog);
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
        targets.clear();
        activeTarget = null;
        spawnTarget();
        spawnTarget();
    }

    private void spawnTarget() {
        String item = TARGET_ITEMS[rng.nextInt(TARGET_ITEMS.length)];
        int w = Math.max(300, canvas.getWidth());
        int h = Math.max(200, canvas.getHeight());

        double x = 60 + rng.nextInt(Math.max(80, w - 240));
        double y = 80 + rng.nextInt(Math.max(60, h - 180));
        double vx = (rng.nextDouble() - 0.5) * 0.8;
        double vy = (rng.nextDouble() - 0.5) * 0.8;

        targets.add(new NinjaTarget(item, x, y, vx, vy));
    }

    @Override
    protected void onGameTick() {
        spawnCounter++;
        if (spawnCounter >= 120) { // ~2 seconds
            spawnCounter = 0;
            if (targets.size() < 4) {
                spawnTarget();
            }
        }

        int w = Math.max(300, canvas.getWidth());
        int h = Math.max(200, canvas.getHeight());

        Iterator<NinjaTarget> it = targets.iterator();
        while (it.hasNext()) {
            NinjaTarget t = it.next();
            t.x += t.vx;
            t.y += t.vy;
            t.life -= 0.016;

            // Bounce off edges
            if (t.x < 40 || t.x > w - 160) t.vx = -t.vx;
            if (t.y < 40 || t.y > h - 80) t.vy = -t.vy;

            if (t.life <= 0) {
                // Target vanished!
                lives--;
                combo = 1;
                SoundManager.error();
                if (activeTarget == t) {
                    activeTarget = null;
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

        if (activeTarget != null) {
            if (activeTarget.typedLen < activeTarget.text.length()) {
                char expected = activeTarget.text.charAt(activeTarget.typedLen);
                if (c == expected) {
                    activeTarget.typedLen++;
                    correctKeystrokes++;
                    SoundManager.keypress();

                    if (activeTarget.typedLen == activeTarget.text.length()) {
                        wordsCompleted++;
                        score += activeTarget.text.length() * 15 * combo;
                        combo = Math.min(6, combo + 1);
                        targets.remove(activeTarget);
                        activeTarget = null;
                        SoundManager.complete();

                        if (wordsCompleted % 6 == 0) {
                            level++;
                        }
                    }
                } else {
                    errors++;
                    combo = 1;
                    SoundManager.error();
                }
            }
            return;
        }

        // Match first char
        for (NinjaTarget t : targets) {
            if (t.text.length() > 0 && t.text.charAt(0) == c) {
                activeTarget = t;
                activeTarget.typedLen = 1;
                correctKeystrokes++;
                SoundManager.keypress();
                if (activeTarget.text.length() == 1) {
                    wordsCompleted++;
                    score += 20 * combo;
                    targets.remove(activeTarget);
                    activeTarget = null;
                    SoundManager.complete();
                }
                return;
            }
        }

        errors++;
        SoundManager.error();
    }

    @Override
    protected void handleBackspace() {
        if (activeTarget != null && activeTarget.typedLen > 0) {
            activeTarget.typedLen--;
            if (activeTarget.typedLen == 0) activeTarget = null;
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

            Font targetFont = ThemeManager.getMonoFont(20f);
            g2.setFont(targetFont);
            FontMetrics fm = g2.getFontMetrics();

            for (NinjaTarget t : targets) {
                int tw = fm.stringWidth(t.text);
                int padX = 14;
                int padY = 8;
                int boxX = (int) t.x - padX;
                int boxY = (int) t.y - fm.getAscent() - padY;
                int boxW = tw + (padX * 2);
                int boxH = fm.getHeight() + (padY * 2);

                boolean isLocked = (t == activeTarget);

                // Background
                g2.setColor(isLocked ? (ThemeManager.isDark() ? new Color(0x3B2D05) : new Color(0xFEF3C7))
                                     : ThemeManager.bgCard());
                g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);

                g2.setColor(isLocked ? ThemeManager.colorWarning() : ThemeManager.borderColor());
                g2.setStroke(new BasicStroke(isLocked ? 2.5f : 1f));
                g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);

                // Life timer bar above target
                int barW = (int) (boxW * (t.life / 8.0));
                g2.setColor(t.life < 2.5 ? ThemeManager.colorError() : ThemeManager.accent());
                g2.fillRect(boxX, boxY - 4, Math.max(0, barW), 3);

                // Text
                int curX = (int) t.x;
                for (int i = 0; i < t.text.length(); i++) {
                    char ch = t.text.charAt(i);
                    String chStr = String.valueOf(ch);
                    int chW = fm.stringWidth(chStr);

                    if (isLocked && i < t.typedLen) {
                        g2.setColor(ThemeManager.colorSuccess());
                    } else if (isLocked && i == t.typedLen) {
                        g2.setColor(ThemeManager.colorWarning());
                    } else {
                        g2.setColor(ThemeManager.fgPrimary());
                    }

                    g2.drawString(chStr, curX, (int) t.y);
                    curX += chW;
                }
            }

            g2.dispose();
        }
    }
}
