package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class KeyboardTrainerPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final VirtualKeyboard keyboard = new VirtualKeyboard();
    private final JLabel prompt = new JLabel("Press the highlighted key");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel streakLabel = new JLabel("Streak: 0");
    private final JLabel bestLabel = new JLabel("Best: 0");
    private int score = 0, streak = 0, best = 0;
    private char current;
    private final Random rng = new Random();
    private final String pool = "asdfjkl;ghqwertyuiopzxcvbnm";

    public KeyboardTrainerPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 6));
        top.setOpaque(false);
        prompt.setFont(prompt.getFont().deriveFont(Font.BOLD, 20f));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 16f));
        streakLabel.setFont(streakLabel.getFont().deriveFont(Font.BOLD, 16f));
        bestLabel.setFont(bestLabel.getFont().deriveFont(Font.BOLD, 16f));

        top.add(prompt);
        top.add(scoreLabel);
        top.add(streakLabel);
        top.add(bestLabel);
        add(top, BorderLayout.NORTH);

        add(keyboard, BorderLayout.CENTER);

        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED || c < 32) return;
                handle(c);
            }
        });

        applyTheme();
        nextKey();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void nextKey() {
        current = pool.charAt(rng.nextInt(pool.length()));
        keyboard.highlightKey(current);
        prompt.setText("Press Key: '" + Character.toUpperCase(current) + "'");
    }

    private void handle(char c) {
        if (Character.toLowerCase(c) == current) {
            score++;
            streak++;
            best = Math.max(best, streak);
            scoreLabel.setText("Score: " + score);
            streakLabel.setText("Streak: " + streak);
            bestLabel.setText("Best: " + best);
            SoundManager.keypress();
            nextKey();
        } else {
            streak = 0;
            keyboard.flashWrong(c);
            streakLabel.setText("Streak: 0");
            SoundManager.error();
        }
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        prompt.setForeground(ThemeManager.accent());
        scoreLabel.setForeground(ThemeManager.fgPrimary());
        streakLabel.setForeground(ThemeManager.fgPrimary());
        bestLabel.setForeground(ThemeManager.fgMuted());
        keyboard.applyTheme();
        repaint();
    }

    @Override
    public void refresh() {
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }
}
