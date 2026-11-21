package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.CharacterError;
import com.typex.model.DailyStat;
import com.typex.model.TypingResult;
import com.typex.service.StatisticsService;
import com.typex.ui.MainFrame;
import com.typex.ui.components.StatCard;
import com.typex.util.ExportManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class StatisticsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final StatisticsService svc = new StatisticsService();

    // Summary cards
    private final StatCard bestWpmCard = new StatCard("Personal Best", "-");
    private final StatCard avgWpmCard = new StatCard("Average WPM", "-");
    private final StatCard avgAccCard = new StatCard("Average Acc", "-");
    private final StatCard totalPracticeCard = new StatCard("Practice Time", "0h 0m");
    private final StatCard consistencyCard = new StatCard("Avg Consistency", "-");
    private final StatCard weakKeyCard = new StatCard("Most Mistyped", "-");

    // Charts
    private final ChartPanel wpmChartPanel;
    private final ChartPanel accChartPanel;
    private final ChartPanel durationChartPanel;
    private final ChartPanel errorChartPanel;
    private final ChartPanel weeklyChartPanel;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JButton csvBtn = new JButton("Export CSV");
    private final JButton reportBtn = new JButton("Printable Report");

    public StatisticsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top summary cards grid
        JPanel cardsGrid = new JPanel(new GridLayout(1, 6, 8, 8));
        cardsGrid.setOpaque(false);
        cardsGrid.add(bestWpmCard);
        cardsGrid.add(avgWpmCard);
        cardsGrid.add(avgAccCard);
        cardsGrid.add(totalPracticeCard);
        cardsGrid.add(consistencyCard);
        cardsGrid.add(weakKeyCard);
        add(cardsGrid, BorderLayout.NORTH);

        // Tab 1: WPM Progression
        JFreeChart wpmChart = ChartFactory.createLineChart(
                "WPM Progression (Recent Tests)", "Test", "Net WPM", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        wpmChartPanel = new ChartPanel(wpmChart);

        // Tab 2: Accuracy Progression
        JFreeChart accChart = ChartFactory.createLineChart(
                "Accuracy Progression (Recent Tests)", "Test", "Accuracy %", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        accChartPanel = new ChartPanel(accChart);

        // Tab 3: Daily Practice Duration
        JFreeChart durationChart = ChartFactory.createBarChart(
                "Daily Practice Duration (Minutes)", "Date", "Minutes", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        durationChartPanel = new ChartPanel(durationChart);

        // Tab 4: Error Distribution
        JFreeChart errorChart = ChartFactory.createBarChart(
                "Most Frequently Mistyped Characters", "Character", "Mistake Count", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        errorChartPanel = new ChartPanel(errorChart);

        // Tab 5: Weekly Performance
        JFreeChart weeklyChart = ChartFactory.createBarChart(
                "Daily Performance Over Last 14 Days", "Date", "Average WPM", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        weeklyChartPanel = new ChartPanel(weeklyChart);

        tabbedPane.addTab("📈 WPM Progression", wpmChartPanel);
        tabbedPane.addTab("🎯 Accuracy Progression", accChartPanel);
        tabbedPane.addTab("⏱ Practice Duration", durationChartPanel);
        tabbedPane.addTab("⚠️ Error Distribution", errorChartPanel);
        tabbedPane.addTab("📅 14-Day Performance", weeklyChartPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom export bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        bottom.setOpaque(false);

        csvBtn.setFocusPainted(false);
        csvBtn.addActionListener(e -> exportCsv());
        reportBtn.setFocusPainted(false);
        reportBtn.addActionListener(e -> exportPrintable());

        bottom.add(csvBtn);
        bottom.add(reportBtn);
        add(bottom, BorderLayout.SOUTH);

        applyTheme();
        refresh();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        bestWpmCard.applyTheme();
        avgWpmCard.applyTheme();
        avgAccCard.applyTheme();
        totalPracticeCard.applyTheme();
        consistencyCard.applyTheme();
        weakKeyCard.applyTheme();

        csvBtn.setBackground(ThemeManager.bgCard());
        csvBtn.setForeground(ThemeManager.fgPrimary());
        reportBtn.setBackground(ThemeManager.accent());
        reportBtn.setForeground(ThemeManager.fgOnAccent());

        styleChart(wpmChartPanel.getChart(), true);
        styleChart(accChartPanel.getChart(), true);
        styleChart(durationChartPanel.getChart(), false);
        styleChart(errorChartPanel.getChart(), false);
        styleChart(weeklyChartPanel.getChart(), false);

        tabbedPane.setBackground(ThemeManager.bgCard());
        tabbedPane.setForeground(ThemeManager.fgPrimary());

        repaint();
    }

    private void styleChart(JFreeChart chart, boolean isLine) {
        if (chart == null) return;
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

        if (isLine) {
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setSeriesPaint(0, ThemeManager.accent());
            renderer.setSeriesStroke(0, new BasicStroke(2.5f));
            plot.setRenderer(renderer);
        } else {
            BarRenderer renderer = new BarRenderer();
            renderer.setSeriesPaint(0, ThemeManager.accent());
            plot.setRenderer(renderer);
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("typex-analytics-report.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fc.getSelectedFile();
            final int uid = frame.getUser().id;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    List<TypingResult> rs = svc.recentFor(uid);
                    ExportManager.exportCsv(targetFile, rs);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(StatisticsPanel.this,
                                "Analytics CSV exported successfully:\n" + targetFile.getAbsolutePath(),
                                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StatisticsPanel.this,
                                "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void exportPrintable() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("typex-analytics-report.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fc.getSelectedFile();
            final int uid = frame.getUser().id;
            final String username = frame.getUser().username;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    List<TypingResult> rs = svc.recentFor(uid);
                    ExportManager.exportPrintableReport(targetFile, uid, username, rs);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(StatisticsPanel.this,
                                "Printable report generated successfully:\n" + targetFile.getAbsolutePath(),
                                "Report Written", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StatisticsPanel.this,
                                "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<AnalyticsBundle, Void> worker = new SwingWorker<>() {
            @Override
            protected AnalyticsBundle doInBackground() {
                long totalSecs = svc.totalPracticeSeconds(uid);
                TypingResult best = svc.bestFor(uid);
                List<TypingResult> recentResults = svc.recentFor(uid);
                List<DailyStat> recentDays = svc.lastDays(uid, 14);
                List<CharacterError> topErrors = svc.getTopErrors(uid, 10);
                return new AnalyticsBundle(totalSecs, best, recentResults, recentDays, topErrors);
            }

            @Override
            protected void done() {
                try {
                    AnalyticsBundle data = get();

                    // Update Cards
                    long secs = data.totalPracticeSecs;
                    totalPracticeCard.setValue(String.format("%dh %dm", secs / 3600, (secs % 3600) / 60));
                    bestWpmCard.setValue(data.best == null ? "-" : String.format("%.0f WPM", data.best.netWpm));

                    double avgW = data.recentResults.stream().mapToDouble(r -> r.netWpm).average().orElse(0.0);
                    double avgA = data.recentResults.stream().mapToDouble(r -> r.accuracy).average().orElse(0.0);
                    double avgC = data.recentResults.stream().mapToDouble(r -> r.consistency).filter(c -> c > 0).average().orElse(0.0);

                    avgWpmCard.setValue(String.format("%.0f WPM", avgW));
                    avgAccCard.setValue(String.format("%.0f%%", avgA));
                    consistencyCard.setValue(avgC > 0 ? String.format("%.0f%%", avgC) : "-");

                    if (!data.topErrors.isEmpty()) {
                        weakKeyCard.setValue("'" + data.topErrors.get(0).charDisplay + "'");
                    } else {
                        weakKeyCard.setValue("None");
                    }

                    // 1. WPM Progression
                    DefaultCategoryDataset wpmDs = new DefaultCategoryDataset();
                    List<TypingResult> chronological = new java.util.ArrayList<>(data.recentResults);
                    Collections.reverse(chronological);
                    int testNum = 1;
                    for (TypingResult r : chronological) {
                        wpmDs.addValue(r.netWpm, "Net WPM", "#" + (testNum++));
                    }
                    if (wpmDs.getRowCount() == 0) wpmDs.addValue(0, "Net WPM", "None");
                    wpmChartPanel.getChart().getCategoryPlot().setDataset(wpmDs);

                    // 2. Accuracy Progression
                    DefaultCategoryDataset accDs = new DefaultCategoryDataset();
                    testNum = 1;
                    for (TypingResult r : chronological) {
                        accDs.addValue(r.accuracy, "Accuracy %", "#" + (testNum++));
                    }
                    if (accDs.getRowCount() == 0) accDs.addValue(100, "Accuracy %", "None");
                    accChartPanel.getChart().getCategoryPlot().setDataset(accDs);

                    // 3. Daily Practice Duration
                    DefaultCategoryDataset durationDs = new DefaultCategoryDataset();
                    for (DailyStat d : data.recentDays) {
                        String label = d.date.length() > 5 ? d.date.substring(5) : d.date;
                        durationDs.addValue(d.practiceSeconds / 60.0, "Minutes", label);
                    }
                    if (durationDs.getRowCount() == 0) durationDs.addValue(0, "Minutes", "Today");
                    durationChartPanel.getChart().getCategoryPlot().setDataset(durationDs);

                    // 4. Error Distribution
                    DefaultCategoryDataset errDs = new DefaultCategoryDataset();
                    for (CharacterError ce : data.topErrors) {
                        errDs.addValue(ce.errorCount, "Errors", ce.charDisplay);
                    }
                    if (errDs.getRowCount() == 0) errDs.addValue(0, "Errors", "No Mistakes");
                    errorChartPanel.getChart().getCategoryPlot().setDataset(errDs);

                    // 5. 14-Day Performance
                    DefaultCategoryDataset weeklyDs = new DefaultCategoryDataset();
                    for (DailyStat d : data.recentDays) {
                        String label = d.date.length() > 5 ? d.date.substring(5) : d.date;
                        weeklyDs.addValue(d.avgWpm, "Avg WPM", label);
                    }
                    if (weeklyDs.getRowCount() == 0) weeklyDs.addValue(0, "Avg WPM", "Today");
                    weeklyChartPanel.getChart().getCategoryPlot().setDataset(weeklyDs);

                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private static class AnalyticsBundle {
        final long totalPracticeSecs;
        final TypingResult best;
        final List<TypingResult> recentResults;
        final List<DailyStat> recentDays;
        final List<CharacterError> topErrors;

        AnalyticsBundle(long totalPracticeSecs, TypingResult best, List<TypingResult> recentResults, List<DailyStat> recentDays, List<CharacterError> topErrors) {
            this.totalPracticeSecs = totalPracticeSecs;
            this.best = best;
            this.recentResults = recentResults;
            this.recentDays = recentDays;
            this.topErrors = topErrors;
        }
    }
}
