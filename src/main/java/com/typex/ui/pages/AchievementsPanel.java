package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.Achievement;
import com.typex.service.AchievementService;
import com.typex.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AchievementsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private static final String[] CATEGORIES = {
            "All Categories", "Speed", "Accuracy", "Consistency", "Learning",
            "Practice Time", "Streak", "Keyboard Mastery"
    };

    private final MainFrame frame;
    private final AchievementService svc = new AchievementService();
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JScrollPane scrollPane;
    private final JComboBox<String> filterBox = new JComboBox<>(CATEGORIES);
    private final JLabel summaryLabel = new JLabel("Unlocked: 0 / 0");
    private List<Achievement> cachedAchievements = new ArrayList<>();

    public AchievementsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top Filter Bar
        JPanel topBar = new JPanel(new BorderLayout(12, 12));
        topBar.setOpaque(false);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        JLabel filterLbl = new JLabel("Category:");
        filterLbl.setFont(filterLbl.getFont().deriveFont(Font.BOLD, 13f));
        filterLbl.setForeground(ThemeManager.fgPrimary());
        leftPanel.add(filterLbl);

        filterBox.addActionListener(e -> renderCards());
        leftPanel.add(filterBox);
        topBar.add(leftPanel, BorderLayout.WEST);

        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, 14f));
        summaryLabel.setForeground(ThemeManager.accent());
        topBar.add(summaryLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        grid.setOpaque(false);
        scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        applyTheme();
        refresh();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        summaryLabel.setForeground(ThemeManager.accent());
        renderCards();
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
                    cachedAchievements = get();
                    long unlockedCount = cachedAchievements.stream().filter(a -> a.unlocked).count();
                    summaryLabel.setText(String.format("Unlocked: %d / %d", unlockedCount, cachedAchievements.size()));
                    renderCards();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void renderCards() {
        grid.removeAll();
        String selected = (String) filterBox.getSelectedItem();
        boolean filterAll = selected == null || selected.equals("All Categories");

        for (Achievement a : cachedAchievements) {
            if (filterAll || (a.category != null && a.category.equalsIgnoreCase(selected))) {
                grid.add(createCard(a));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createCard(Achievement a) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(ThemeManager.bgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(a.unlocked ? ThemeManager.accent() : (ThemeManager.isDark() ? new Color(0x2E2E2E) : new Color(0xE5E7EB)), a.unlocked ? 2 : 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        // Top: Icon + Category Badge
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel icon = new JLabel(a.icon == null ? "🏆" : a.icon, SwingConstants.CENTER);
        icon.setFont(icon.getFont().deriveFont(32f));
        cardHeader.add(icon, BorderLayout.CENTER);

        JLabel catBadge = new JLabel(a.category != null ? a.category : "General", SwingConstants.RIGHT);
        catBadge.setFont(catBadge.getFont().deriveFont(Font.PLAIN, 10f));
        catBadge.setForeground(ThemeManager.fgMuted());
        cardHeader.add(catBadge, BorderLayout.NORTH);

        JLabel title = new JLabel(a.title, SwingConstants.CENTER);
        title.setForeground(a.unlocked ? ThemeManager.fgPrimary() : ThemeManager.fgMuted());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JLabel desc = new JLabel("<html><div style='text-align:center'>" + a.description + "</div></html>",
                SwingConstants.CENTER);
        desc.setForeground(ThemeManager.fgMuted());

        JLabel xp = new JLabel((a.unlocked ? "✓ Unlocked · " : "🔒 Locked · ") + "+" + a.xp + " XP", SwingConstants.CENTER);
        xp.setFont(xp.getFont().deriveFont(Font.BOLD, 12f));
        xp.setForeground(a.unlocked ? ThemeManager.accent() : ThemeManager.fgMuted());

        p.add(cardHeader, BorderLayout.NORTH);
        p.add(title, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setOpaque(false);
        bottom.add(desc, BorderLayout.NORTH);
        bottom.add(xp, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }
}
