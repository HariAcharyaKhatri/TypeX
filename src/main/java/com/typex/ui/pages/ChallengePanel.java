package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChallengePanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final List<ChallengeCard> cards = new ArrayList<>();

    public ChallengePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
        grid.setOpaque(false);

        grid.add(createCard("⚡ Speed Challenge", "Type as fast as you can for 60 seconds with steady rhythm.", "speed", 60));
        grid.add(createCard("🎯 Accuracy Challenge", "Maintain 100% precision for 30 seconds without misstrokes.", "accuracy", 30));
        grid.add(createCard("💎 Perfect Typing", "Zero mistakes allowed for 15 seconds of intense focus.", "perfect", 15));
        grid.add(createCard("⏱ Time Attack", "120-second sustained typing endurance drill.", "time", 120));
        add(grid, BorderLayout.NORTH);

        JLabel note = new JLabel("<html><i>Challenges run offline. Choose any drill to push your speed and accuracy to the next level.</i></html>");
        note.setForeground(ThemeManager.fgMuted());
        add(note, BorderLayout.SOUTH);

        applyTheme();
    }

    private JPanel createCard(String title, String desc, String mode, int seconds) {
        ChallengeCard p = new ChallengeCard(title, desc, () -> frame.navigate("test"));
        cards.add(p);
        return p;
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        for (ChallengeCard c : cards) {
            c.applyTheme();
        }
        repaint();
    }

    @Override
    public void refresh() {}

    private static class ChallengeCard extends JPanel {
        private final JLabel titleLabel = new JLabel();
        private final JLabel descLabel = new JLabel();
        private final JButton startBtn = new JButton("Start Challenge");

        public ChallengeCard(String title, String desc, Runnable onStart) {
            setLayout(new BorderLayout(8, 8));
            titleLabel.setText(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

            descLabel.setText("<html>" + desc + "</html>");
            descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 13f));

            startBtn.setFocusPainted(false);
            startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            startBtn.addActionListener(e -> onStart.run());

            add(titleLabel, BorderLayout.NORTH);
            add(descLabel, BorderLayout.CENTER);
            add(startBtn, BorderLayout.SOUTH);

            applyTheme();
        }

        public void applyTheme() {
            setBackground(ThemeManager.bgCard());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x2E2E2E) : new Color(0xE5E7EB)),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));
            titleLabel.setForeground(ThemeManager.accent());
            descLabel.setForeground(ThemeManager.fgPrimary());
            startBtn.setBackground(ThemeManager.accent());
            startBtn.setForeground(Color.BLACK);
        }
    }
}
