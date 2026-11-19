package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.DailyStat;
import com.typex.model.TypingResult;
import com.typex.service.StatisticsService;
import com.typex.ui.MainFrame;
import com.typex.util.ExportManager;
import org.jfree.chart.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class StatisticsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final StatisticsService svc = new StatisticsService();
    private final ChartPanel chartPanel;
    private final JButton csvBtn = new JButton("Export CSV");
    private final JButton reportBtn = new JButton("Printable Report");

    public StatisticsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "WPM Over Last 14 Days", "Date", "WPM", ds, PlotOrientation.VERTICAL, false, true, false);
        chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

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
        csvBtn.setBackground(ThemeManager.bgCard());
        csvBtn.setForeground(ThemeManager.fgPrimary());
        reportBtn.setBackground(ThemeManager.accent());
        reportBtn.setForeground(Color.BLACK);

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

            BarRenderer renderer = new BarRenderer();
            renderer.setSeriesPaint(0, ThemeManager.accent());
            plot.setRenderer(renderer);

            chartPanel.setBackground(ThemeManager.bgCard());
        }
        repaint();
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("typex-report.csv"));
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
                        JOptionPane.showMessageDialog(StatisticsPanel.this, "CSV Exported successfully:\n" + targetFile.getAbsolutePath(),
                                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StatisticsPanel.this, "Export failed: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void exportPrintable() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("typex-report.txt"));
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
                        JOptionPane.showMessageDialog(StatisticsPanel.this, "Printable report written to:\n" + targetFile.getAbsolutePath(),
                                "Report Written", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(StatisticsPanel.this, "Export failed: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
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

        SwingWorker<List<DailyStat>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DailyStat> doInBackground() {
                return svc.lastDays(uid, 14);
            }

            @Override
            protected void done() {
                try {
                    List<DailyStat> recent = get();
                    DefaultCategoryDataset ds = new DefaultCategoryDataset();
                    if (recent.isEmpty()) {
                        ds.addValue(0, "WPM", "Today");
                    } else {
                        for (DailyStat d : recent) {
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
}
