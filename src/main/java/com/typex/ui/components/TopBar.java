package com.typex.ui.components;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TopBar extends JPanel {
    private final MainFrame frame;
    private final JLabel userLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JButton themeBtn;
    private final JButton profileBtn;

    public TopBar(MainFrame frame, Runnable onTheme, Runnable onProfile) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        add(dateLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        themeBtn = pill("🌓 Theme");
        themeBtn.addActionListener(e -> onTheme.run());

        profileBtn = pill("👤 Profile");
        profileBtn.addActionListener(e -> onProfile.run());

        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        right.add(themeBtn);
        right.add(userLabel);
        right.add(profileBtn);
        add(right, BorderLayout.EAST);

        applyTheme();
        refresh();
    }

    public void applyTheme() {
        setBackground(ThemeManager.isDark() ? new Color(0x1A1A1A) : Color.WHITE);
        dateLabel.setForeground(ThemeManager.fgMuted());
        userLabel.setForeground(ThemeManager.fgPrimary());
        updatePill(themeBtn);
        updatePill(profileBtn);
        repaint();
    }

    public void refresh() {
        LocalDate now = LocalDate.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy")));
        if (frame != null && frame.getUser() != null) {
            try {
                com.typex.model.UserXP xp = new com.typex.dao.UserXPDAO().getForUser(frame.getUser().id);
                com.typex.model.PracticeStreak streak = new com.typex.dao.PracticeStreakDAO().getForUser(frame.getUser().id);
                String streakText = streak.currentStreak > 0 ? " · 🔥 " + streak.currentStreak : "";
                userLabel.setText(String.format("👤 %s (Lvl %d%s)", frame.getUser().username, xp.level, streakText));
            } catch (Exception e) {
                userLabel.setText("👤 " + frame.getUser().username);
            }
        }
    }

    public void setUserText(String t) {
        userLabel.setText(t);
    }

    private JButton pill(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        updatePill(b);
        return b;
    }

    private void updatePill(JButton b) {
        if (b == null) return;
        b.setBackground(ThemeManager.bgCard());
        b.setForeground(ThemeManager.fgPrimary());
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x3A3A3A) : new Color(0xD1D5DB)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }
}
