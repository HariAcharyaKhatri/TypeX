package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.TypingResultDAO;
import com.typex.model.*;
import com.typex.service.GamificationService;
import com.typex.service.RecommendationService;
import com.typex.service.StatisticsService;
import com.typex.ui.MainFrame;
import com.typex.ui.components.StatCard;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final StatisticsService statsSvc = new StatisticsService();
    private final GamificationService gamification = new GamificationService();
    private final RecommendationService recSvc = new RecommendationService();
    private final TypingResultDAO resultDao = new TypingResultDAO();

    // Welcome Banner
    private final JPanel welcomeBanner = new JPanel(new BorderLayout(16, 8));
    private final JLabel welcomeTitle = new JLabel("Welcome back!");
    private final JLabel welcomeSubtitle = new JLabel("Ready to elevate your typing speed and accuracy today?");
    private final JButton bannerQuickStart = new JButton("Start Typing Test", com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PLAY, 14, Color.WHITE));

    // 4 Key Stat Cards
    private final StatCard avgWpm = new StatCard("AVG SPEED (7D)", "-", com.typex.util.IconManager.getIcon(com.typex.util.IconManager.SPEED, 16, new Color(0x2563EB)));
    private final StatCard bestWpm = new StatCard("PERSONAL BEST", "-", com.typex.util.IconManager.getIcon(com.typex.util.IconManager.TROPHY, 16, new Color(0xF59E0B)));
    private final StatCard accuracy = new StatCard("ACCURACY (7D)", "-", com.typex.util.IconManager.getIcon(com.typex.util.IconManager.ACCURACY, 16, new Color(0x10B981)));
    private final StatCard streak = new StatCard("ACTIVE STREAK", "0", com.typex.util.IconManager.getIcon(com.typex.util.IconManager.FLAME, 16, new Color(0xD97706)));

    // Middle Row: Level & XP Card + Daily Goal Card
    private final JPanel levelCard = new JPanel(new BorderLayout(8, 8));
    private final JLabel levelTitle = new JLabel("Level 1 Typist");
    private final JLabel xpValue = new JLabel("0 XP");
    private final JProgressBar levelProgress = new JProgressBar(0, 100);

    private final JPanel goalCard = new JPanel(new BorderLayout(8, 8));
    private final JLabel streakLabel = new JLabel("0 Day Streak");
    private final JLabel goalStatus = new JLabel("Daily Goal: 0% complete");
    private final JProgressBar goalProgress = new JProgressBar(0, 100);

    // Smart Recommendation Banner
    private final JPanel recCard = new JPanel(new BorderLayout(14, 12));
    private final JLabel recTitle = new JLabel("Recommended Practice");
    private final JLabel recDesc = new JLabel("Loading smart practice recommendations...");
    private final JLabel recReason = new JLabel("");
    private final JButton recActionBtn = new JButton("Practice Now");
    private PracticeRecommendation currentRec;

    // Progress Chart
    private final ChartPanel chartPanel;

    // Recent Tests Table
    private final JPanel recentTestsPanel = new JPanel(new BorderLayout(8, 8));
    private final DefaultTableModel recentTableModel = new DefaultTableModel(
            new String[]{"Date / Time", "Category", "Duration", "Net Speed", "Accuracy"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable recentTable = new JTable(recentTableModel);

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel scrollRoot = new JPanel();
        scrollRoot.setLayout(new BoxLayout(scrollRoot, BoxLayout.Y_AXIS));
        scrollRoot.setOpaque(false);
        scrollRoot.setBorder(BorderFactory.createEmptyBorder(18, 20, 24, 20));

        // 1. Welcome Banner
        setupWelcomeBanner();
        scrollRoot.add(welcomeBanner);
        scrollRoot.add(Box.createVerticalStrut(14));

        // 2. 4 Key Stat Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 12));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        statsRow.add(avgWpm);
        statsRow.add(bestWpm);
        statsRow.add(accuracy);
        statsRow.add(streak);
        scrollRoot.add(statsRow);
        scrollRoot.add(Box.createVerticalStrut(14));

        // 3. Middle Row: Level & XP Card + Daily Goal Card
        JPanel gamificationRow = new JPanel(new GridLayout(1, 2, 12, 12));
        gamificationRow.setOpaque(false);
        gamificationRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        setupLevelCard();
        setupGoalCard();
        gamificationRow.add(levelCard);
        gamificationRow.add(goalCard);
        scrollRoot.add(gamificationRow);
        scrollRoot.add(Box.createVerticalStrut(14));

        // 4. Smart Recommendation Banner
        setupRecommendationCard();
        recCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        scrollRoot.add(recCard);
        scrollRoot.add(Box.createVerticalStrut(14));

        // 5. Progress Chart
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Speed Progression (WPM)", "Day", "WPM", ds, PlotOrientation.VERTICAL, false, true, false);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(700, 240));
        chartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        scrollRoot.add(chartPanel);
        scrollRoot.add(Box.createVerticalStrut(14));

        // 6. Recent Activity
        setupRecentTestsPanel();
        recentTestsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        scrollRoot.add(recentTestsPanel);

        JScrollPane mainScroll = new JScrollPane(scrollRoot);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        applyTheme();
        refresh();
    }

    private void setupWelcomeBanner() {
        welcomeBanner.setBackground(ThemeManager.bgCard());
        welcomeBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        welcomeBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        welcomeTitle.setFont(ThemeManager.getTitleFont(20f));
        welcomeSubtitle.setFont(ThemeManager.getBodyFont(13f));
        textCol.add(welcomeTitle);
        textCol.add(Box.createVerticalStrut(3));
        textCol.add(welcomeSubtitle);

        bannerQuickStart.setFont(ThemeManager.getTitleFont(14f));
        bannerQuickStart.setBackground(ThemeManager.accent());
        bannerQuickStart.setForeground(ThemeManager.fgOnAccent());
        bannerQuickStart.setFocusPainted(false);
        bannerQuickStart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bannerQuickStart.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        bannerQuickStart.addActionListener(e -> frame.navigate("test"));

        welcomeBanner.add(textCol, BorderLayout.CENTER);
        welcomeBanner.add(bannerQuickStart, BorderLayout.EAST);
    }

    private void setupLevelCard() {
        levelCard.setBackground(ThemeManager.bgCard());
        levelCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        levelTitle.setFont(ThemeManager.getTitleFont(14f));
        xpValue.setFont(ThemeManager.getTitleFont(13f));
        top.add(levelTitle, BorderLayout.WEST);
        top.add(xpValue, BorderLayout.EAST);

        levelProgress.setStringPainted(true);
        levelProgress.setForeground(ThemeManager.accent());
        levelProgress.setPreferredSize(new Dimension(0, 18));

        levelCard.add(top, BorderLayout.NORTH);
        levelCard.add(levelProgress, BorderLayout.CENTER);
    }

    private void setupGoalCard() {
        goalCard.setBackground(ThemeManager.bgCard());
        goalCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        streakLabel.setFont(ThemeManager.getTitleFont(14f));
        goalStatus.setFont(ThemeManager.getBodyFont(12f));
        top.add(streakLabel, BorderLayout.WEST);
        top.add(goalStatus, BorderLayout.EAST);

        goalProgress.setStringPainted(true);
        goalProgress.setForeground(ThemeManager.colorSuccess());
        goalProgress.setPreferredSize(new Dimension(0, 18));

        goalCard.add(top, BorderLayout.NORTH);
        goalCard.add(goalProgress, BorderLayout.CENTER);
    }

    private void setupRecommendationCard() {
        recCard.setBackground(ThemeManager.bgCard());
        recCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.accentLight(), 1),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        recTitle.setFont(ThemeManager.getTitleFont(13.5f));
        recTitle.setForeground(ThemeManager.accent());
        recDesc.setFont(ThemeManager.getBodyFont(13f));
        recReason.setFont(ThemeManager.getBodyFont(11.5f));

        textPanel.add(recTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(recDesc);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(recReason);

        recActionBtn.setFont(ThemeManager.getTitleFont(13f));
        recActionBtn.setBackground(ThemeManager.accent());
        recActionBtn.setForeground(ThemeManager.fgOnAccent());
        recActionBtn.setFocusPainted(false);
        recActionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recActionBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        recActionBtn.addActionListener(e -> {
            if (currentRec != null && currentRec.actionPage != null) {
                frame.navigate(currentRec.actionPage);
            } else {
                frame.navigate("test");
            }
        });

        recCard.add(textPanel, BorderLayout.CENTER);
        recCard.add(recActionBtn, BorderLayout.EAST);
    }

    private void setupRecentTestsPanel() {
        recentTestsPanel.setBackground(ThemeManager.bgCard());
        recentTestsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel title = new JLabel("RECENT TYPING SESSIONS");
        title.setFont(ThemeManager.getTitleFont(11.5f));
        title.setForeground(ThemeManager.fgMuted());
        recentTestsPanel.add(title, BorderLayout.NORTH);

        recentTable.setFont(ThemeManager.getBodyFont(12.5f));
        recentTable.setRowHeight(26);
        recentTable.setShowGrid(false);
        recentTable.setFillsViewportHeight(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        recentTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane tableScroll = new JScrollPane(recentTable);
        tableScroll.setBorder(null);
        tableScroll.getViewport().setBackground(ThemeManager.bgCard());
        recentTestsPanel.add(tableScroll, BorderLayout.CENTER);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());

        welcomeBanner.setBackground(ThemeManager.bgCard());
        welcomeTitle.setForeground(ThemeManager.fgPrimary());
        welcomeSubtitle.setForeground(ThemeManager.fgSecondary());
        bannerQuickStart.setBackground(ThemeManager.accent());
        bannerQuickStart.setForeground(ThemeManager.fgOnAccent());

        avgWpm.applyTheme();
        bestWpm.applyTheme();
        accuracy.applyTheme();
        streak.applyTheme();

        levelCard.setBackground(ThemeManager.bgCard());
        levelTitle.setForeground(ThemeManager.fgPrimary());
        xpValue.setForeground(ThemeManager.accent());

        goalCard.setBackground(ThemeManager.bgCard());
        streakLabel.setForeground(ThemeManager.colorGold());
        goalStatus.setForeground(ThemeManager.fgMuted());

        recCard.setBackground(ThemeManager.accentLight());
        recTitle.setForeground(ThemeManager.accent());
        recDesc.setForeground(ThemeManager.fgPrimary());
        recReason.setForeground(ThemeManager.fgSecondary());
        recActionBtn.setBackground(ThemeManager.accent());
        recActionBtn.setForeground(ThemeManager.fgOnAccent());

        recentTestsPanel.setBackground(ThemeManager.bgCard());
        recentTable.setBackground(ThemeManager.bgCard());
        recentTable.setForeground(ThemeManager.fgPrimary());
        recentTable.getTableHeader().setBackground(ThemeManager.bgSecondary());
        recentTable.getTableHeader().setForeground(ThemeManager.fgSecondary());
        recentTable.getTableHeader().setFont(ThemeManager.getTitleFont(11.5f));

        if (chartPanel != null && chartPanel.getChart() != null) {
            JFreeChart chart = chartPanel.getChart();
            chart.setBackgroundPaint(ThemeManager.bgCard());
            if (chart.getTitle() != null) {
                chart.getTitle().setPaint(ThemeManager.fgPrimary());
                chart.getTitle().setFont(ThemeManager.getTitleFont(14f));
            }
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(ThemeManager.bgCard());
            plot.setOutlinePaint(ThemeManager.borderColor());
            plot.setRangeGridlinePaint(ThemeManager.borderColor());
            plot.setDomainGridlinePaint(ThemeManager.borderColor());
            plot.getDomainAxis().setLabelPaint(ThemeManager.fgSecondary());
            plot.getDomainAxis().setTickLabelPaint(ThemeManager.fgMuted());
            plot.getRangeAxis().setLabelPaint(ThemeManager.fgSecondary());
            plot.getRangeAxis().setTickLabelPaint(ThemeManager.fgMuted());

            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setSeriesPaint(0, ThemeManager.accent());
            renderer.setSeriesStroke(0, new BasicStroke(2.5f));
            plot.setRenderer(renderer);

            chartPanel.setBackground(ThemeManager.bgCard());
        }
        repaint();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;
        welcomeTitle.setText("Welcome back, " + frame.getUser().username + "!");

        SwingWorker<DashboardFullData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardFullData doInBackground() {
                long secs = statsSvc.totalPracticeSeconds(uid);
                TypingResult best = statsSvc.bestFor(uid);
                List<DailyStat> recent = statsSvc.lastDays(uid, 7);
                int testCount = statsSvc.testCount(uid);
                int streakCount = statsSvc.streak(uid);

                UserXP xp = gamification.getUserXP(uid);
                PracticeStreak pStreak = gamification.getStreak(uid);
                DailyGoal goal = gamification.getDailyGoal(uid);
                List<PracticeRecommendation> recs = recSvc.getRecommendations(uid);
                PracticeRecommendation topRec = recs.isEmpty() ? null : recs.get(0);
                List<TypingResult> recentTests = resultDao.findByUser(uid, 5);

                return new DashboardFullData(secs, best, recent, testCount, streakCount, xp, pStreak, goal, topRec, recentTests);
            }

            @Override
            protected void done() {
                try {
                    DashboardFullData data = get();

                    // 1. Level & XP
                    levelTitle.setText("Level " + data.xp.level + " Typist");
                    xpValue.setText(data.xp.totalXp + " XP");
                    int lvlProg = (int) Math.round(data.xp.progressPercent);
                    levelProgress.setValue(lvlProg);
                    int remainingXp = Math.max(0, data.xp.nextLevelXp - data.xp.totalXp);
                    levelProgress.setString(remainingXp > 0
                            ? String.format("%d XP to Level %d (%d%%)", remainingXp, data.xp.level + 1, lvlProg)
                            : "Max Level Reached");

                    // 2. Streak & Daily Goal
                    int curStreak = data.streak.currentStreak;
                    streakLabel.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.FLAME, 16, ThemeManager.colorGold()));
                    streakLabel.setText(String.format("%d Day Streak (Best: %d)", curStreak, data.streak.longestStreak));
                    int goalProg = (int) Math.round(data.goal.getProgressPercentage());
                    goalProgress.setValue(goalProg);
                    goalStatus.setText(data.goal.achieved ? "✓ Daily Goal Achieved! (+150 XP)" : String.format("Goal: %d%% completed", goalProg));
                    goalProgress.setString(String.format("%d/%d tests · %d/%d lessons · %dm/%dm",
                            data.goal.completedTests, data.goal.targetTests,
                            data.goal.completedLessons, data.goal.targetLessons,
                            data.goal.completedSeconds / 60, data.goal.targetMinutes));

                    // 3. Recommendation
                    currentRec = data.recommendation;
                    if (currentRec != null) {
                        recTitle.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.LIGHTBULB, 16, ThemeManager.accent()));
                        recTitle.setText(currentRec.title);
                        recDesc.setText(currentRec.description);
                        recReason.setText("Focus: " + currentRec.reason);
                        recActionBtn.setText(currentRec.buttonText);
                        recActionBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PLAY, 13, ThemeManager.fgOnAccent()));
                        recCard.setVisible(true);
                    } else {
                        recCard.setVisible(false);
                    }

                    // 4. Quick Stats
                    bestWpm.setValue(data.best == null ? "--" : String.format("%.0f WPM", data.best.netWpm));
                    double avgW = data.recent.stream().mapToDouble(d -> d.avgWpm).average().orElse(0);
                    double avgA = data.recent.stream().mapToDouble(d -> d.avgAccuracy).average().orElse(0);
                    avgWpm.setValue(avgW > 0 ? String.format("%.0f WPM", avgW) : "--");
                    accuracy.setValue(avgA > 0 ? String.format("%.1f%%", avgA) : "--");
                    streak.setValue(curStreak + " Days");

                    // 5. Chart
                    DefaultCategoryDataset ds = new DefaultCategoryDataset();
                    if (data.recent.isEmpty()) {
                        ds.addValue(0, "WPM", "No Activity");
                    } else {
                        for (DailyStat d : data.recent) {
                            String label = d.date.length() > 5 ? d.date.substring(5) : d.date;
                            ds.addValue(d.avgWpm, "WPM", label);
                        }
                    }
                    chartPanel.getChart().getCategoryPlot().setDataset(ds);

                    // 6. Recent Tests Table
                    recentTableModel.setRowCount(0);
                    if (data.recentTests != null && !data.recentTests.isEmpty()) {
                        for (TypingResult tr : data.recentTests) {
                            String dateStr = tr.completedAt != null && tr.completedAt.length() > 16
                                    ? tr.completedAt.substring(5, 16) : String.valueOf(tr.completedAt);
                            recentTableModel.addRow(new Object[]{
                                    dateStr,
                                    tr.category,
                                    tr.durationSeconds + "s",
                                    String.format("%.1f WPM", tr.netWpm),
                                    String.format("%.1f%%", tr.accuracy)
                            });
                        }
                    } else {
                        recentTableModel.addRow(new Object[]{"--", "No sessions yet", "--", "--", "--"});
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private static class DashboardFullData {
        final long practiceSecs;
        final TypingResult best;
        final List<DailyStat> recent;
        final int testCount;
        final int streakCount;
        final UserXP xp;
        final PracticeStreak streak;
        final DailyGoal goal;
        final PracticeRecommendation recommendation;
        final List<TypingResult> recentTests;

        DashboardFullData(long practiceSecs, TypingResult best, List<DailyStat> recent, int testCount, int streakCount,
                          UserXP xp, PracticeStreak streak, DailyGoal goal, PracticeRecommendation recommendation,
                          List<TypingResult> recentTests) {
            this.practiceSecs = practiceSecs;
            this.best = best;
            this.recent = recent;
            this.testCount = testCount;
            this.streakCount = streakCount;
            this.xp = xp;
            this.streak = streak;
            this.goal = goal;
            this.recommendation = recommendation;
            this.recentTests = recentTests;
        }
    }
}
