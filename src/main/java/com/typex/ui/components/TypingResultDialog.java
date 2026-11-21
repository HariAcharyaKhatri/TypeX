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
        this(parent, r, isNewPersonalBest, previousPbWpm, null, newlyUnlocked, onTryAgain, null);
    }

    public TypingResultDialog(Window parent, TypingResult r, boolean isNewPersonalBest, double previousPbWpm,
                              GamificationService.SessionReward reward, List<Achievement> newlyUnlocked, Runnable onTryAgain) {
        this(parent, r, isNewPersonalBest, previousPbWpm, reward, newlyUnlocked, onTryAgain, null);
    }

    public TypingResultDialog(Window parent, TypingResult r, boolean isNewPersonalBest, double previousPbWpm,
                              GamificationService.SessionReward reward, List<Achievement> newlyUnlocked,
                              Runnable onTryAgain, Runnable onNewTest) {
        super(parent, "Test Results", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(12, 12));
        setSize(600, 580);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(ThemeManager.bgApp());
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Test Complete!", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(ThemeManager.getTitleFont(24f));
        title.setForeground(ThemeManager.fgPrimary());
        header.add(title);
        header.add(Box.createVerticalStrut(4));

        if (reward != null && reward.leveledUp) {
            JLabel lvlBanner = new JLabel("🎉 LEVEL UP! You reached Level " + reward.newLevel + "! 🎉", SwingConstants.CENTER);
            lvlBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
            lvlBanner.setFont(ThemeManager.getTitleFont(15f));
            lvlBanner.setForeground(ThemeManager.colorSuccess());
            header.add(lvlBanner);
            header.add(Box.createVerticalStrut(2));
        }

        if (isNewPersonalBest) {
            JLabel pbBanner = new JLabel("⭐ NEW PERSONAL BEST! ⭐", SwingConstants.CENTER);
            pbBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
            pbBanner.setFont(ThemeManager.getTitleFont(15f));
            pbBanner.setForeground(ThemeManager.colorGold());
            header.add(pbBanner);
        } else if (previousPbWpm > 0) {
            JLabel pbNote = new JLabel(String.format("Personal Best: %.1f WPM", previousPbWpm), SwingConstants.CENTER);
            pbNote.setAlignmentX(Component.CENTER_ALIGNMENT);
            pbNote.setFont(ThemeManager.getBodyFont(13f));
            pbNote.setForeground(ThemeManager.fgMuted());
            header.add(pbNote);
        }

        if (reward != null && reward.xpGained > 0) {
            JLabel xpBadge = new JLabel("+" + reward.xpGained + " XP Earned" +
                    (reward.goalCompleted ? " · 🎯 Daily Goal Completed!" : "") +
                    (reward.currentStreak > 1 ? " · 🔥 " + reward.currentStreak + " Day Streak" : ""), SwingConstants.CENTER);
            xpBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            xpBadge.setFont(ThemeManager.getTitleFont(13f));
            xpBadge.setForeground(ThemeManager.accent());
            header.add(Box.createVerticalStrut(3));
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
            chTitle.setFont(ThemeManager.getTitleFont(13f));
            chTitle.setForeground(ThemeManager.accent());
            notices.add(chTitle);
            for (UserChallenge uc : reward.completedChallenges) {
                JLabel l = new JLabel("  🎯 " + uc.challenge.title + " (+" + uc.challenge.xpReward + " XP)");
                l.setFont(ThemeManager.getBodyFont(12.5f));
                l.setForeground(ThemeManager.fgPrimary());
                notices.add(l);
            }
            notices.add(Box.createVerticalStrut(4));
        }

        List<Achievement> achs = (reward != null && reward.newlyUnlocked != null && !reward.newlyUnlocked.isEmpty())
                ? reward.newlyUnlocked : newlyUnlocked;

        if (achs != null && !achs.isEmpty()) {
            JLabel achTitle = new JLabel("Achievements Unlocked:");
            achTitle.setFont(ThemeManager.getTitleFont(13f));
            achTitle.setForeground(ThemeManager.colorGold());
            notices.add(achTitle);
            for (Achievement a : achs) {
                JLabel l = new JLabel("  " + (a.icon != null ? a.icon : "🏆") + " " + a.title + " (+" + a.xp + " XP)");
                l.setFont(ThemeManager.getBodyFont(12.5f));
                l.setForeground(ThemeManager.fgPrimary());
                notices.add(l);
            }
        }

        if (notices.getComponentCount() > 0) {
            bottom.add(notices, BorderLayout.NORTH);
        }

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton tryAgainBtn = new JButton("↻ Try Again");
        tryAgainBtn.setFont(ThemeManager.getTitleFont(13f));
        tryAgainBtn.setBackground(ThemeManager.accent());
        tryAgainBtn.setForeground(ThemeManager.fgOnAccent());
        tryAgainBtn.setFocusPainted(false);
        tryAgainBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tryAgainBtn.addActionListener(e -> {
            dispose();
            if (onTryAgain != null) onTryAgain.run();
        });

        JButton newTestBtn = new JButton("New Test");
        newTestBtn.setFont(ThemeManager.getBodyFont(13f));
        newTestBtn.setBackground(ThemeManager.bgCard());
        newTestBtn.setForeground(ThemeManager.fgPrimary());
        newTestBtn.setFocusPainted(false);
        newTestBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        newTestBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newTestBtn.addActionListener(e -> {
            dispose();
            if (onNewTest != null) onNewTest.run();
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(ThemeManager.getBodyFont(13f));
        closeBtn.setBackground(ThemeManager.bgCard());
        closeBtn.setForeground(ThemeManager.fgSecondary());
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        closeBtn.addActionListener(e -> dispose());

        btnBar.add(tryAgainBtn);
        btnBar.add(newTestBtn);
        btnBar.add(closeBtn);
        bottom.add(btnBar, BorderLayout.SOUTH);

        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel card(String label, String value, boolean highlight) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(ThemeManager.bgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(ThemeManager.getBodyFont(12f));
        lbl.setForeground(ThemeManager.fgMuted());

        JLabel val = new JLabel(value);
        val.setFont(ThemeManager.getTitleFont(highlight ? 18f : 16f));
        val.setForeground(highlight ? ThemeManager.accent() : ThemeManager.fgPrimary());

        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }
}
