package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.Achievement;
import com.typex.service.AchievementService;
import com.typex.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AchievementsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final AchievementService svc = new AchievementService();
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JScrollPane scrollPane;

    public AchievementsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        grid.setOpaque(false);
        scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        applyTheme();
        refresh();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        refresh();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<List<Achievement>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Achievement> doInBackground() {
                return svc.forUser(uid);
            }

            @Override
            protected void done() {
                try {
                    List<Achievement> list = get();
                    grid.removeAll();
                    for (Achievement a : list) {
                        grid.add(createCard(a));
                    }
                    grid.revalidate();
                    grid.repaint();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private JPanel createCard(Achievement a) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(ThemeManager.bgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(a.unlocked ? ThemeManager.accent() : (ThemeManager.isDark() ? new Color(0x2E2E2E) : new Color(0xE5E7EB)), a.unlocked ? 2 : 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel icon = new JLabel(a.icon == null ? "🏆" : a.icon, SwingConstants.CENTER);
        icon.setFont(icon.getFont().deriveFont(32f));
        icon.setForeground(a.unlocked ? ThemeManager.accent() : ThemeManager.fgMuted());

        JLabel title = new JLabel(a.title, SwingConstants.CENTER);
        title.setForeground(a.unlocked ? ThemeManager.fgPrimary() : ThemeManager.fgMuted());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JLabel desc = new JLabel("<html><div style='text-align:center'>" + a.description + "</div></html>",
                SwingConstants.CENTER);
        desc.setForeground(ThemeManager.fgMuted());

        JLabel xp = new JLabel((a.unlocked ? "✓ Unlocked · " : "🔒 Locked · ") + "+" + a.xp + " XP", SwingConstants.CENTER);
        xp.setFont(xp.getFont().deriveFont(Font.BOLD, 12f));
        xp.setForeground(a.unlocked ? ThemeManager.accent() : ThemeManager.fgMuted());

        p.add(icon, BorderLayout.NORTH);
        p.add(title, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setOpaque(false);
        bottom.add(desc, BorderLayout.NORTH);
        bottom.add(xp, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }
}
