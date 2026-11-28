package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;
import com.typex.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TopBar extends JPanel {
    private final MainFrame frame;
    private final JLabel userBadge = new JLabel();
    private final JLabel streakBadge = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JButton themeBtn;
    private final JButton soundBtn;
    private final JButton profileBtn;

    public TopBar(MainFrame frame, Runnable onTheme, Runnable onProfile) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 52));

        dateLabel.setFont(ThemeManager.getBodyFont(13f));
        add(dateLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        streakBadge.setFont(ThemeManager.getTitleFont(12.5f));
        streakBadge.setOpaque(true);
        streakBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        streakBadge.setIcon(IconManager.getIcon(IconManager.FLAME, 14, new Color(0xD97706)));
        streakBadge.setIconTextGap(6);

        userBadge.setFont(ThemeManager.getTitleFont(12.5f));
        userBadge.setOpaque(true);
        userBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        userBadge.setIcon(IconManager.getIcon(IconManager.PROFILE, 14, ThemeManager.accent()));
        userBadge.setIconTextGap(6);

        soundBtn = pill(com.typex.util.SoundManager.isEnabled() ? "Sound" : "Muted",
                com.typex.util.SoundManager.isEnabled() ? IconManager.SOUND_ON : IconManager.SOUND_OFF);
        soundBtn.setToolTipText("Toggle Sound Effects (Ctrl+M)");
        soundBtn.addActionListener(e -> {
            com.typex.util.SoundManager.toggleEnabled();
            updateSoundPill();
        });

        themeBtn = pill("Theme", IconManager.THEME);
        themeBtn.setToolTipText("Toggle Dark/Light Mode (Ctrl+T)");
        themeBtn.addActionListener(e -> onTheme.run());

        profileBtn = pill("Profile", IconManager.PROFILE);
        profileBtn.setToolTipText("Manage Profiles & Goals (Ctrl+P)");
        profileBtn.addActionListener(e -> onProfile.run());

        right.add(streakBadge);
        right.add(userBadge);
        right.add(soundBtn);
        right.add(themeBtn);
        right.add(profileBtn);
        add(right, BorderLayout.EAST);

        applyTheme();
        refresh();
    }

    private void updateSoundPill() {
        if (soundBtn == null) return;
        boolean on = com.typex.util.SoundManager.isEnabled();
        soundBtn.setText(on ? "Sound" : "Muted");
        updatePill(soundBtn, on ? IconManager.SOUND_ON : IconManager.SOUND_OFF);
    }

    public void applyTheme() {
        setBackground(ThemeManager.bgCard());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        dateLabel.setForeground(ThemeManager.fgSecondary());

        // Streak badge styling
        streakBadge.setBackground(ThemeManager.isDark() ? new Color(0x3B2508) : new Color(0xFEF3C7));
        streakBadge.setForeground(new Color(0xD97706));
        streakBadge.setIcon(IconManager.getIcon(IconManager.FLAME, 14, new Color(0xD97706)));

        // User & Level badge styling
        userBadge.setBackground(ThemeManager.accentLight());
        userBadge.setForeground(ThemeManager.accent());
        userBadge.setIcon(IconManager.getIcon(IconManager.PROFILE, 14, ThemeManager.accent()));

        updateSoundPill();
        updatePill(themeBtn, IconManager.THEME);
        updatePill(profileBtn, IconManager.PROFILE);
        repaint();
    }

    public void refresh() {
        LocalDate now = LocalDate.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        updateSoundPill();
        if (frame != null && frame.getUser() != null) {
            try {
                com.typex.model.UserXP xp = new com.typex.dao.UserXPDAO().getForUser(frame.getUser().id);
                com.typex.model.PracticeStreak streak = new com.typex.dao.PracticeStreakDAO().getForUser(frame.getUser().id);

                if (streak.currentStreak > 0) {
                    streakBadge.setText(streak.currentStreak + " Day Streak");
                    streakBadge.setVisible(true);
                } else {
                    streakBadge.setVisible(false);
                }

                userBadge.setText(String.format("%s  ·  Lvl %d (%d XP)",
                        frame.getUser().username, xp.level, xp.totalXp));
            } catch (Exception e) {
                userBadge.setText(frame.getUser().username);
                streakBadge.setVisible(false);
            }
        }
    }

    public void setUserText(String t) {
        userBadge.setText(t);
    }

    private JButton pill(String text, String iconKey) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(ThemeManager.getBodyFont(13f));
        b.setIcon(IconManager.getIcon(iconKey, 14, ThemeManager.fgPrimary()));
        b.setIconTextGap(6);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updatePill(b, iconKey);
        return b;
    }

    private void updatePill(JButton b, String iconKey) {
        if (b == null) return;
        b.setBackground(ThemeManager.bgCard());
        b.setForeground(ThemeManager.fgPrimary());
        b.setIcon(IconManager.getIcon(iconKey, 14, ThemeManager.fgPrimary()));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
    }
}
