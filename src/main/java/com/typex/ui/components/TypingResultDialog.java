package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.model.Achievement;
import com.typex.model.TypingResult;
import com.typex.model.UserChallenge;
import com.typex.service.GamificationService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TypingResultDialog extends JDialog {

    public TypingResultDialog(Window parent, TypingResult r, boolean isNewPersonalBest, double previousPbWpm,
                              List<Achievement> newlyUnlocked, Runnable onTryAgain) {
        this(parent, r, isNewPersonalBest, previousPbWpm, null, newlyUnlocked, onTryAgain);
    }

    public TypingResultDialog(Window parent, TypingResult r, boolean isNewPersonalBest, double previousPbWpm,
                              GamificationService.SessionReward reward, List<Achievement> newlyUnlocked, Runnable onTryAgain) {
        super(parent, "Test Results", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(12, 12));
        setSize(580, 560);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(ThemeManager.bgApp());
        root.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Test Complete!", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(ThemeManager.fgPrimary());
        header.add(title);
        header.add(Box.createVerticalStrut(4));

        if (reward != null && reward.leveledUp) {
            JLabel lvlBanner = new JLabel("🎉 LEVEL UP! You reached Level " + reward.newLevel + "! 🎉", SwingConstants.CENTER);
            lvlBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
            lvlBanner.setFont(lvlBanner.getFont().deriveFont(Font.BOLD, 16f));
            lvlBanner.setForeground(new Color(0x10B981));
            header.add(lvlBanner);
            header.add(Box.createVerticalStrut(2));
        }

        if (isNewPersonalBest) {
            JLabel pbBanner = new JLabel("⭐ NEW PERSONAL BEST! ⭐", SwingConstants.CENTER);
            pbBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
            pbBanner.setFont(pbBanner.getFont().deriveFont(Font.BOLD, 15f));
            pbBanner.setForeground(new Color(0xF59E0B));
            header.add(pbBanner);
        } else if (previousPbWpm > 0) {
            JLabel pbNote = new JLabel(String.format("Personal Best: %.1f WPM", previousPbWpm), SwingConstants.CENTER);
            pbNote.setAlignmentX(Component.CENTER_ALIGNMENT);
            pbNote.setForeground(ThemeManager.fgMuted());
            header.add(pbNote);
        }

        if (reward != null && reward.xpGained > 0) {
            JLabel xpBadge = new JLabel("+" + reward.xpGained + " XP Earned" +
                    (reward.goalCompleted ? " · 🎯 Daily Goal Completed!" : "") +
                    (reward.currentStreak > 1 ? " · 🔥 " + reward.currentStreak + " Day Streak" : ""), SwingConstants.CENTER);
            xpBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            xpBadge.setFont(xpBadge.getFont().deriveFont(Font.BOLD, 13f));
            xpBadge.setForeground(ThemeManager.accent());
            header.add(Box.createVerticalStrut(2));
            header.add(xpBadge);
        }

        root.add(header, BorderLayout.NORTH);

        // Grid of stats
        JPanel grid = new JPanel(new GridLayout(4, 2, 10, 10));
        grid.setOpaque(false);

        grid.add(card("Net Speed", String.format("%.1f WPM", r.netWpm), true));
        grid.add(card("Gross Speed", String.format("%.1f WPM", r.grossWpm), false));
        grid.add(card("Accuracy", String.format("%.1f%%", r.accuracy), true));
        grid.add(card("Raw Accuracy", String.format("%.1f%%", r.rawAccuracy), false));
        grid.add(card("Keystrokes (Total / Raw)", String.format("%d / %d", r.totalKeystrokes, r.rawKeystrokes), false));
        grid.add(card("Errors (Unfixed / Fixed)", String.format("%d / %d", r.uncorrectedErrors, r.correctedErrors), false));
        grid.add(card("Typing Consistency", String.format("%.1f%%", r.consistency), false));
        grid.add(card("Backspaces", String.valueOf(r.backspaces), false));

        root.add(grid, BorderLayout.CENTER);

        // Bottom: Unlocked Achievements / Completed Challenges + Buttons
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);

        JPanel notices = new JPanel();
        notices.setLayout(new BoxLayout(notices, BoxLayout.Y_AXIS));
        notices.setOpaque(false);

        if (reward != null && reward.completedChallenges != null && !reward.completedChallenges.isEmpty()) {
            JLabel chTitle = new JLabel("Missions Completed:");
            chTitle.setFont(chTitle.getFont().deriveFont(Font.BOLD, 13f));
            chTitle.setForeground(ThemeManager.accent());
            notices.add(chTitle);
            for (UserChallenge uc : reward.completedChallenges) {
                JLabel l = new JLabel("  🎯 " + uc.challenge.title + " (+" + uc.challenge.xpReward + " XP)");
                l.setForeground(ThemeManager.fgPrimary());
                notices.add(l);
            }
            notices.add(Box.createVerticalStrut(4));
        }

        List<Achievement> achs = (reward != null && reward.newlyUnlocked != null && !reward.newlyUnlocked.isEmpty())
                ? reward.newlyUnlocked : newlyUnlocked;

        if (achs != null && !achs.isEmpty()) {
            JLabel achTitle = new JLabel("Achievements Unlocked:");
            achTitle.setFont(achTitle.getFont().deriveFont(Font.BOLD, 13f));
            achTitle.setForeground(ThemeManager.accent());
            notices.add(achTitle);
            for (Achievement a : achs) {
                JLabel l = new JLabel("  " + (a.icon != null ? a.icon : "🏆") + " " + a.title + " (+" + a.xp + " XP)");
                l.setForeground(ThemeManager.fgPrimary());
                notices.add(l);
            }
        }

        if (notices.getComponentCount() > 0) {
            bottom.add(notices, BorderLayout.NORTH);
        }

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton tryAgainBtn = new JButton("Try Again");
        tryAgainBtn.setBackground(ThemeManager.accent());
        tryAgainBtn.setForeground(Color.BLACK);
        tryAgainBtn.setFocusPainted(false);
        tryAgainBtn.addActionListener(e -> {
            dispose();
            if (onTryAgain != null) onTryAgain.run();
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());

        btnBar.add(tryAgainBtn);
        btnBar.add(closeBtn);
        bottom.add(btnBar, BorderLayout.SOUTH);

        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel card(String label, String value, boolean highlight) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(ThemeManager.bgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(ThemeManager.fgMuted());

        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.BOLD, highlight ? 18f : 16f));
        val.setForeground(highlight ? ThemeManager.accent() : ThemeManager.fgPrimary());

        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }
}
