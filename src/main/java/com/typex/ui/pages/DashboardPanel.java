package com.typex.ui.pages;

import com.typex.config.ThemeManager;
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
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final StatisticsService statsSvc = new StatisticsService();
    private final GamificationService gamification = new GamificationService();
    private final RecommendationService recSvc = new RecommendationService();

    // Gamification Cards
    private final JPanel levelCard = new JPanel(new BorderLayout(8, 8));
    private final JLabel levelTitle = new JLabel("Level 1 Typist");
    private final JLabel xpValue = new JLabel("0 XP");
    private final JProgressBar levelProgress = new JProgressBar(0, 100);

    private final JPanel goalCard = new JPanel(new BorderLayout(8, 8));
    private final JLabel streakLabel = new JLabel("🔥 0 Day Streak");
    private final JLabel goalStatus = new JLabel("Daily Goal: 0% complete");
    private final JProgressBar goalProgress = new JProgressBar(0, 100);

    // Smart Recommendation
    private final JPanel recCard = new JPanel(new BorderLayout(12, 12));
    private final JLabel recTitle = new JLabel("💡 Recommended Practice");
    private final JLabel recDesc = new JLabel("Loading smart practice recommendations...");
    private final JLabel recReason = new JLabel("");
    private final JButton recActionBtn = new JButton("Practice Now");
    private PracticeRecommendation currentRec;

    // Stat Cards
    private final StatCard totalTime = new StatCard("Practice Time", "0h");
    private final StatCard bestWpm = new StatCard("Best WPM", "-");
    private final StatCard avgWpm = new StatCard("Avg WPM (7d)", "-");
    private final StatCard accuracy = new StatCard("Avg Accuracy", "-");
    private final StatCard tests = new StatCard("Tests", "0");
    private final StatCard streak = new StatCard("Streak", "0");

    // Center Chart
    private final ChartPanel chartPanel;
    private final JButton quickStart = new JButton("Start Typing Test");

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel scrollRoot = new JPanel();
        scrollRoot.setLayout(new BoxLayout(scrollRoot, BoxLayout.Y_AXIS));
        scrollRoot.setOpaque(false);

        // 1. Top Gamification Row (Level Card + Daily Goal Card)
        JPanel gamificationRow = new JPanel(new GridLayout(1, 2, 12, 12));
        gamificationRow.setOpaque(false);
        gamificationRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        setupLevelCard();
        setupGoalCard();
        gamificationRow.add(levelCard);
        gamificationRow.add(goalCard);
        scrollRoot.add(gamificationRow);
        scrollRoot.add(Box.createVerticalStrut(12));

        // 2. Smart Recommendation Banner
        setupRecommendationCard();
        recCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        scrollRoot.add(recCard);
        scrollRoot.add(Box.createVerticalStrut(12));

        // 3. Stats Grid
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        cards.add(totalTime); cards.add(bestWpm); cards.add(avgWpm);
        cards.add(accuracy); cards.add(tests); cards.add(streak);
        scrollRoot.add(cards);
        scrollRoot.add(Box.createVerticalStrut(12));

        // 4. Progress Chart
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Weekly WPM Progress", "Day", "WPM", ds, PlotOrientation.VERTICAL, false, true, false);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(700, 260));
        chartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollRoot.add(chartPanel);

        JScrollPane mainScroll = new JScrollPane(scrollRoot);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        // Bottom Actions Bar
        quickStart.setFont(quickStart.getFont().deriveFont(Font.BOLD, 14f));
        quickStart.setFocusPainted(false);
        quickStart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        quickStart.addActionListener(e -> frame.navigate("test"));

        JButton challengesBtn = new JButton("View Missions");
        challengesBtn.setFocusPainted(false);
        challengesBtn.addActionListener(e -> frame.navigate("challenge"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        bottom.add(challengesBtn);
        bottom.add(quickStart);
        add(bottom, BorderLayout.SOUTH);

        applyTheme();
        refresh();
    }

    private void setupLevelCard() {
        levelCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        levelTitle.setFont(levelTitle.getFont().deriveFont(Font.BOLD, 15f));
        xpValue.setFont(xpValue.getFont().deriveFont(Font.BOLD, 13f));
        top.add(levelTitle, BorderLayout.WEST);
        top.add(xpValue, BorderLayout.EAST);

        levelProgress.setStringPainted(true);
        levelProgress.setForeground(ThemeManager.accent());

        levelCard.add(top, BorderLayout.NORTH);
        levelCard.add(levelProgress, BorderLayout.CENTER);
    }

    private void setupGoalCard() {
        goalCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        streakLabel.setFont(streakLabel.getFont().deriveFont(Font.BOLD, 15f));
        goalStatus.setFont(goalStatus.getFont().deriveFont(Font.PLAIN, 12f));
        top.add(streakLabel, BorderLayout.WEST);
        top.add(goalStatus, BorderLayout.EAST);

        goalProgress.setStringPainted(true);
        goalProgress.setForeground(new Color(0x10B981));

        goalCard.add(top, BorderLayout.NORTH);
        goalCard.add(goalProgress, BorderLayout.CENTER);
    }

    private void setupRecommendationCard() {
        recCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.accent(), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        recTitle.setFont(recTitle.getFont().deriveFont(Font.BOLD, 14f));
        recTitle.setForeground(ThemeManager.accent());
        recDesc.setFont(recDesc.getFont().deriveFont(Font.PLAIN, 13f));
        recDesc.setForeground(ThemeManager.fgPrimary());
        recReason.setFont(recReason.getFont().deriveFont(Font.ITALIC, 11f));
        recReason.setForeground(ThemeManager.fgMuted());

        textPanel.add(recTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(recDesc);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(recReason);

        recActionBtn.setFont(recActionBtn.getFont().deriveFont(Font.BOLD, 12f));
        recActionBtn.setFocusPainted(false);
        recActionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        totalTime.applyTheme();
        bestWpm.applyTheme();
        avgWpm.applyTheme();
        accuracy.applyTheme();
        tests.applyTheme();
        streak.applyTheme();

        levelCard.setBackground(ThemeManager.bgCard());
        levelTitle.setForeground(ThemeManager.fgPrimary());
        xpValue.setForeground(ThemeManager.accent());

        goalCard.setBackground(ThemeManager.bgCard());
        streakLabel.setForeground(new Color(0xF59E0B));
        goalStatus.setForeground(ThemeManager.fgMuted());

        recCard.setBackground(ThemeManager.bgCard());
        recTitle.setForeground(ThemeManager.accent());
        recDesc.setForeground(ThemeManager.fgPrimary());
        recReason.setForeground(ThemeManager.fgMuted());
        recActionBtn.setBackground(ThemeManager.accent());
        recActionBtn.setForeground(Color.BLACK);

        quickStart.setBackground(ThemeManager.accent());
        quickStart.setForeground(Color.BLACK);

        if (chartPanel != null && chartPanel.getChart() != null) {
            JFreeChart chart = chartPanel.getChart();
            chart.setBackgroundPaint(ThemeManager.bgCard());
            if (chart.getTitle() != null) {
                chart.getTitle().setPaint(ThemeManager.fgPrimary());
            }
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(ThemeManager.bgCard());
            plot.setOutlinePaint(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xDDDDDD));
            plot.setRangeGridlinePaint(ThemeManager.isDark() ? new Color(0x444444) : new Color(0xCCCCCC));
            plot.setDomainGridlinePaint(ThemeManager.isDark() ? new Color(0x444444) : new Color(0xCCCCCC));
            plot.getDomainAxis().setLabelPaint(ThemeManager.fgMuted());
            plot.getDomainAxis().setTickLabelPaint(ThemeManager.fgMuted());
            plot.getRangeAxis().setLabelPaint(ThemeManager.fgMuted());
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

                return new DashboardFullData(secs, best, recent, testCount, streakCount, xp, pStreak, goal, topRec);
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
                            : "Max Level");

                    // 2. Streak & Daily Goal
                    int curStreak = data.streak.currentStreak;
                    streakLabel.setText(String.format("🔥 %d Day Streak (Best: %d)", curStreak, data.streak.longestStreak));
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
                        recTitle.setText("💡 " + currentRec.title);
                        recDesc.setText(currentRec.description);
                        recReason.setText("Why: " + currentRec.reason);
                        recActionBtn.setText(currentRec.buttonText);
                        recCard.setVisible(true);
                    } else {
                        recCard.setVisible(false);
                    }

                    // 4. Quick Stats
                    long secs = data.practiceSecs;
                    totalTime.setValue(String.format("%dh %dm", secs / 3600, (secs % 3600) / 60));
                    bestWpm.setValue(data.best == null ? "-" : String.format("%.0f", data.best.netWpm));
                    double avgW = data.recent.stream().mapToDouble(d -> d.avgWpm).average().orElse(0);
                    double avgA = data.recent.stream().mapToDouble(d -> d.avgAccuracy).average().orElse(0);
                    avgWpm.setValue(String.format("%.0f", avgW));
                    accuracy.setValue(String.format("%.0f%%", avgA));
                    tests.setValue(String.valueOf(data.testCount));
                    streak.setValue(String.valueOf(curStreak));

                    // 5. Chart
                    DefaultCategoryDataset ds = new DefaultCategoryDataset();
                    if (data.recent.isEmpty()) {
                        ds.addValue(0, "WPM", "Today");
                    } else {
                        for (DailyStat d : data.recent) {
                            String label = d.date.length() > 5 ? d.date.substring(5) : d.date;
                            ds.addValue(d.avgWpm, "WPM", label);
                        }
                    }
                    chartPanel.getChart().getCategoryPlot().setDataset(ds);
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

        DashboardFullData(long practiceSecs, TypingResult best, List<DailyStat> recent, int testCount, int streakCount,
                          UserXP xp, PracticeStreak streak, DailyGoal goal, PracticeRecommendation recommendation) {
            this.practiceSecs = practiceSecs;
            this.best = best;
            this.recent = recent;
            this.testCount = testCount;
            this.streakCount = streakCount;
            this.xp = xp;
            this.streak = streak;
            this.goal = goal;
            this.recommendation = recommendation;
        }
    }
}
