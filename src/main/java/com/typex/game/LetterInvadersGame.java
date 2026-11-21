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
 * Letter Invaders: Classic arcade space defense typing game.
 * Strike target keys to fire lasers and protect Earth's perimeter!
 */
public class LetterInvadersGame extends BaseGamePanel {

    private static class Invader {
        char letter;
        double x;
        double y;
        boolean alive = true;

        Invader(char letter, double x, double y) {
            this.letter = letter;
            this.x = x;
            this.y = y;
        }
    }

    private static class Laser {
        double startX, startY;
        double targetX, targetY;
        double currentX, currentY;
        double progress = 0; // 0 to 1
        boolean finished = false;

        Laser(double startX, double startY, double targetX, double targetY) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.currentX = startX;
            this.currentY = startY;
        }
    }

    private final GameCanvas canvas = new GameCanvas();
    private final List<Invader> invaders = new ArrayList<>();
    private final List<Laser> lasers = new ArrayList<>();
    private final Random rng = new Random();

    private int shields = 3;
    private double dirX = 1.0;
    private double formationSpeed = 1.0;
    private int wave = 1;

    public LetterInvadersGame(MainFrame frame, Runnable onExitToCatalog) {
        super(frame, GameType.LETTER_INVADERS, onExitToCatalog);
    }

    @Override
    protected JComponent getGameComponent() {
        return canvas;
    }

    @Override
    protected String getMetric1Name() {
        return "WAVE";
    }

    @Override
    protected String getMetric1Value() {
        return String.valueOf(wave);
    }

    @Override
    protected String getMetric2Name() {
        return "SHIELDS";
    }

    @Override
    protected String getMetric2Value() {
        return "♥".repeat(Math.max(0, shields));
    }

    @Override
    protected void onGameStart() {
        shields = 3;
        wave = 1;
        formationSpeed = 0.8;
        lasers.clear();
        spawnFormation();
    }

    private void spawnFormation() {
        invaders.clear();
        String pool = "asdfjkl;qwertyuiopzxcvbnm";
        int rows = 3;
        int cols = 6;
        int startX = 60;
        int startY = 40;
        int spacingX = 65;
        int spacingY = 45;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = pool.charAt(rng.nextInt(pool.length()));
                invaders.add(new Invader(ch, startX + (c * spacingX), startY + (r * spacingY)));
            }
        }
    }

    @Override
    protected void onGameTick() {
        int w = Math.max(300, canvas.getWidth());
        int baselineY = Math.max(100, canvas.getHeight() - 60);

        // Update lasers
        Iterator<Laser> lIt = lasers.iterator();
        while (lIt.hasNext()) {
            Laser l = lIt.next();
            l.progress += 0.2;
            l.currentX = l.startX + (l.targetX - l.startX) * l.progress;
            l.currentY = l.startY + (l.targetY - l.startY) * l.progress;
            if (l.progress >= 1.0) {
                lIt.remove();
            }
        }

        // Formation movement
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = 0;

        for (Invader inv : invaders) {
            if (!inv.alive) continue;
            minX = Math.min(minX, inv.x);
            maxX = Math.max(maxX, inv.x);
            maxY = Math.max(maxY, inv.y);
        }

        if (invaders.stream().noneMatch(i -> i.alive)) {
            // Wave cleared!
            wave++;
            level = wave;
            formationSpeed += 0.25;
            score += 250;
            SoundManager.complete();
            spawnFormation();
            return;
        }

        boolean dropDown = false;
        if (maxX >= w - 60 && dirX > 0) {
            dirX = -1.0;
            dropDown = true;
        } else if (minX <= 40 && dirX < 0) {
            dirX = 1.0;
            dropDown = true;
        }

        for (Invader inv : invaders) {
            if (!inv.alive) continue;
            inv.x += dirX * formationSpeed;
            if (dropDown) {
                inv.y += 18;
            }

            if (inv.y >= baselineY) {
                shields--;
                inv.alive = false;
                SoundManager.error();
                if (shields <= 0) {
                    endGame();
                    return;
                }
            }
        }
    }

    @Override
    protected void handleTypedChar(char c) {
        totalKeystrokes++;

        // Find lowest living invader with matching letter
        Invader target = null;
        for (Invader inv : invaders) {
            if (inv.alive && Character.toLowerCase(inv.letter) == Character.toLowerCase(c)) {
                if (target == null || inv.y > target.y) {
                    target = inv;
                }
            }
        }

        if (target != null) {
            target.alive = false;
            wordsCompleted++;
            correctKeystrokes++;
            score += 50;

            // Fire laser from bottom center to target
            int canonX = canvas.getWidth() / 2;
            int canonY = canvas.getHeight() - 40;
            lasers.add(new Laser(canonX, canonY, target.x + 16, target.y + 16));

            SoundManager.keypress();
        } else {
            errors++;
            SoundManager.error();
        }
    }

    @Override
    protected void handleBackspace() {}

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
            int baselineY = h - 60;

            // Defense line
            g2.setColor(ThemeManager.colorError());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6f, 6f}, 0));
            g2.drawLine(20, baselineY, w - 20, baselineY);

            // Defense cannon at bottom
            int canonX = w / 2;
            int canonY = h - 35;
            g2.setColor(ThemeManager.accent());
            g2.fillPolygon(new int[]{canonX - 18, canonX, canonX + 18}, new int[]{canonY + 12, canonY - 14, canonY + 12}, 3);

            // Draw lasers
            g2.setColor(ThemeManager.colorSuccess());
            g2.setStroke(new BasicStroke(3f));
            for (Laser l : lasers) {
                g2.drawLine((int) l.startX, (int) l.startY, (int) l.currentX, (int) l.currentY);
            }

            // Draw invaders
            Font invFont = ThemeManager.getMonoFont(16f);
            g2.setFont(invFont);
            FontMetrics fm = g2.getFontMetrics();

            for (Invader inv : invaders) {
                if (!inv.alive) continue;
                int ix = (int) inv.x;
                int iy = (int) inv.y;
                int boxW = 42;
                int boxH = 34;

                // Alien body
                g2.setColor(ThemeManager.bgCard());
                g2.fillRoundRect(ix, iy, boxW, boxH, 8, 8);
                g2.setColor(ThemeManager.accent());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(ix, iy, boxW, boxH, 8, 8);

                // Letter
                String lStr = String.valueOf(inv.letter);
                int lw = fm.stringWidth(lStr);
                g2.setColor(ThemeManager.fgPrimary());
                g2.drawString(lStr, ix + (boxW - lw) / 2, iy + 22);
            }

            g2.dispose();
        }
    }
}
