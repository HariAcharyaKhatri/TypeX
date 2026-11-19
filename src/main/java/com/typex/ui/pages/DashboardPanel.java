package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.DailyStat;
import com.typex.model.TypingResult;
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
    private final StatisticsService svc = new StatisticsService();
    private final StatCard totalTime = new StatCard("Practice Time", "0h");
    private final StatCard bestWpm = new StatCard("Best WPM", "-");
    private final StatCard avgWpm = new StatCard("Avg WPM (7d)", "-");
    private final StatCard accuracy = new StatCard("Avg Accuracy", "-");
    private final StatCard tests = new StatCard("Tests", "0");
    private final StatCard streak = new StatCard("Streak", "0");
    private final ChartPanel chartPanel;
    private final JButton quickStart = new JButton("Start Typing Test");

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        cards.add(totalTime); cards.add(bestWpm); cards.add(avgWpm);
        cards.add(accuracy); cards.add(tests); cards.add(streak);
        add(cards, BorderLayout.NORTH);

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Weekly WPM Progress", "Day", "WPM", ds, PlotOrientation.VERTICAL, false, true, false);
        chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

        quickStart.setFont(quickStart.getFont().deriveFont(Font.BOLD, 14f));
        quickStart.setFocusPainted(false);
        quickStart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        quickStart.addActionListener(e -> frame.navigate("test"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(quickStart);
        add(bottom, BorderLayout.SOUTH);

        applyTheme();
        refresh();
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

        // Background loading to not block Event Dispatch Thread
        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() {
                long secs = svc.totalPracticeSeconds(uid);
                TypingResult best = svc.bestFor(uid);
                List<DailyStat> recent = svc.lastDays(uid, 7);
                int testCount = svc.testCount(uid);
                int streakCount = svc.streak(uid);
                return new DashboardData(secs, best, recent, testCount, streakCount);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    long secs = data.practiceSecs;
                    totalTime.setValue(String.format("%dh %dm", secs / 3600, (secs % 3600) / 60));
                    bestWpm.setValue(data.best == null ? "-" : String.format("%.0f", data.best.netWpm));

                    double avgW = data.recent.stream().mapToDouble(d -> d.avgWpm).average().orElse(0);
                    double avgA = data.recent.stream().mapToDouble(d -> d.avgAccuracy).average().orElse(0);
                    avgWpm.setValue(String.format("%.0f", avgW));
                    accuracy.setValue(String.format("%.0f%%", avgA));
                    tests.setValue(String.valueOf(data.testCount));
                    streak.setValue(String.valueOf(data.streakCount));

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

    private static class DashboardData {
        final long practiceSecs;
        final TypingResult best;
        final List<DailyStat> recent;
        final int testCount;
        final int streakCount;

        DashboardData(long practiceSecs, TypingResult best, List<DailyStat> recent, int testCount, int streakCount) {
            this.practiceSecs = practiceSecs;
            this.best = best;
            this.recent = recent;
            this.testCount = testCount;
            this.streakCount = streakCount;
        }
    }
}
