package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.Achievement;
import com.typex.model.PersonalBest;
import com.typex.model.TypingResult;
import com.typex.service.AchievementService;
import com.typex.service.StatisticsService;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingResultDialog;
import com.typex.ui.components.TypingTextPanel;
import com.typex.util.SoundManager;
import com.typex.util.TextGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class TypingTestPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private static final String[] DURATIONS = {"15", "30", "60", "120", "180", "300"};
    private static final String[] CATEGORIES = {
            "Random Words", "Common English", "Sentences", "Paragraphs",
            "Quotes", "Business English", "Programming", "Numbers and Symbols"
    };

    private final MainFrame frame;
    private final StatisticsService stats = new StatisticsService();
    private final AchievementService achievements = new AchievementService();

    private final JComboBox<String> durationBox = new JComboBox<>(DURATIONS);
    private final JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
    private final TypingTextPanel textPane = new TypingTextPanel();
    private final JLabel timeLabel = new JLabel("60s");
    private final JLabel wpmLabel = new JLabel("WPM: 0");
    private final JLabel accLabel = new JLabel("Acc: 100%");
    private final JLabel mistakesLabel = new JLabel("Mistakes: 0");
    private final JLabel pbLabel = new JLabel("PB: -");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final JButton startBtn = new JButton("Start Test");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton resumeBtn = new JButton("Resume");
    private final JButton restartBtn = new JButton("Restart");

    private TypingEngine engine;
    private Timer countdown;
    private int selectedDurationSeconds = 60;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private double currentPbWpm = 0.0;

    public TypingTestPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top control bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        top.add(new JLabel("Duration (s):"));
        durationBox.setSelectedItem("60");
        durationBox.addActionListener(e -> updatePbLabel());
        top.add(durationBox);

        top.add(new JLabel("Category:"));
        categoryBox.addActionListener(e -> updatePbLabel());
        top.add(categoryBox);

        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> startTest());
        top.add(startBtn);

        pauseBtn.setFocusPainted(false);
        pauseBtn.setEnabled(false);
        pauseBtn.addActionListener(e -> pauseTest());
        top.add(pauseBtn);

        resumeBtn.setFocusPainted(false);
        resumeBtn.setEnabled(false);
        resumeBtn.addActionListener(e -> resumeTest());
        top.add(resumeBtn);

        restartBtn.setFocusPainted(false);
        restartBtn.setEnabled(false);
        restartBtn.addActionListener(e -> restartTest());
        top.add(restartBtn);

        top.add(pbLabel);
        top.add(statusLabel);
        add(top, BorderLayout.NORTH);

        // Center metrics & text pane
        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        metrics.setOpaque(false);
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD, 22f));
        wpmLabel.setFont(wpmLabel.getFont().deriveFont(Font.BOLD, 18f));
        accLabel.setFont(accLabel.getFont().deriveFont(Font.BOLD, 18f));
        mistakesLabel.setFont(mistakesLabel.getFont().deriveFont(Font.BOLD, 18f));

        metrics.add(timeLabel);
        metrics.add(wpmLabel);
        metrics.add(accLabel);
        metrics.add(mistakesLabel);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xDDDDDD)));
        center.add(scroll, BorderLayout.CENTER);

        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 16));
        center.add(progressBar, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // Focus & Key handling
        textPane.setFocusable(true);
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isRunning && !isPaused) {
                    textPane.requestFocusInWindow();
                }
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (engine == null || !isRunning || isPaused) return;

                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    engine.backspace();
                    e.consume();
                    textPane.render();
                    updateMetrics();
                    return;
                }
                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED || c < 32) return;

                boolean ok = engine.type(c);
                if (ok) {
                    SoundManager.keypress();
                } else {
                    SoundManager.error();
                }
                textPane.render();
                updateMetrics();

                if (engine.isComplete()) {
                    endTest();
                }
            }
        });

        applyTheme();
        setReadyState();
    }

    private void updatePbLabel() {
        if (frame.getUser() == null) return;
        String cat = (String) categoryBox.getSelectedItem();
        int dur = Integer.parseInt((String) durationBox.getSelectedItem());
        SwingWorker<PersonalBest, Void> worker = new SwingWorker<>() {
            @Override
            protected PersonalBest doInBackground() {
                return stats.getPersonalBest(frame.getUser().id, cat, dur);
            }

            @Override
            protected void done() {
                try {
                    PersonalBest pb = get();
                    if (pb != null) {
                        currentPbWpm = pb.bestWpm;
                        pbLabel.setText(String.format("PB: %.1f WPM", pb.bestWpm));
                    } else {
                        currentPbWpm = 0.0;
                        pbLabel.setText("PB: -");
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        startBtn.setBackground(ThemeManager.accent());
        startBtn.setForeground(Color.BLACK);
        restartBtn.setBackground(ThemeManager.bgCard());
        restartBtn.setForeground(ThemeManager.fgPrimary());
        pauseBtn.setBackground(ThemeManager.bgCard());
        pauseBtn.setForeground(ThemeManager.fgPrimary());
        resumeBtn.setBackground(ThemeManager.bgCard());
        resumeBtn.setForeground(ThemeManager.fgPrimary());

        timeLabel.setForeground(ThemeManager.accent());
        wpmLabel.setForeground(ThemeManager.fgPrimary());
        accLabel.setForeground(ThemeManager.fgPrimary());
        mistakesLabel.setForeground(new Color(0xEF4444));
        pbLabel.setForeground(new Color(0xF59E0B));
        statusLabel.setForeground(ThemeManager.fgMuted());
        progressBar.setForeground(ThemeManager.accent());

        textPane.applyTheme();
        repaint();
    }

    private void setReadyState() {
        isRunning = false;
        isPaused = false;
        startBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);
        restartBtn.setEnabled(false);
        durationBox.setEnabled(true);
        categoryBox.setEnabled(true);
        statusLabel.setText("Ready");
        timeLabel.setText(durationBox.getSelectedItem() + "s");
        wpmLabel.setText("WPM: 0");
        accLabel.setText("Acc: 100%");
        mistakesLabel.setText("Mistakes: 0");
        progressBar.setValue(0);
        updatePbLabel();
    }

    private void startTest() {
        if (countdown != null) {
            countdown.stop();
        }

        selectedDurationSeconds = Integer.parseInt((String) durationBox.getSelectedItem());
        String cat = (String) categoryBox.getSelectedItem();
        // Generate enough text for the chosen duration
        int minChars = Math.max(300, selectedDurationSeconds * 12);
        String text = TextGenerator.generate(cat, minChars);

        engine = new TypingEngine(text);
        engine.start();

        textPane.setEngine(engine);
        isRunning = true;
        isPaused = false;

        startBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        restartBtn.setEnabled(true);
        durationBox.setEnabled(false);
        categoryBox.setEnabled(false);
        statusLabel.setText("Typing...");

        updateMetrics();

        // 100ms precision ticker
        countdown = new Timer(100, e -> {
            if (!isRunning || isPaused || engine == null) return;
            double elapsed = engine.elapsedSeconds();
            int remaining = (int) Math.ceil(selectedDurationSeconds - elapsed);
            if (remaining <= 0) {
                timeLabel.setText("0s");
                endTest();
            } else {
                timeLabel.setText(remaining + "s");
                updateMetrics();
                engine.sampleWpm();
            }
        });
        countdown.start();

        SwingUtilities.invokeLater(textPane::requestFocusInWindow);
    }

    private void pauseTest() {
        if (!isRunning || isPaused || engine == null) return;
        isPaused = true;
        engine.pause();
        if (countdown != null) countdown.stop();

        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(true);
        statusLabel.setText("[PAUSED]");
    }

    private void resumeTest() {
        if (!isRunning || !isPaused || engine == null) return;
        isPaused = false;
        engine.resume();
        if (countdown != null) countdown.start();

        pauseBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        statusLabel.setText("Typing...");
        SwingUtilities.invokeLater(textPane::requestFocusInWindow);
    }

    private void restartTest() {
        if (countdown != null) {
            countdown.stop();
        }
        startTest();
    }

    private void updateMetrics() {
        if (engine == null) return;
        wpmLabel.setText(String.format("WPM: %.0f", engine.netWpm()));
        accLabel.setText(String.format("Acc: %.1f%%", engine.accuracy()));
        mistakesLabel.setText("Mistakes: " + engine.getUncorrectedErrors());
        int prog = (int) Math.round(engine.progressPercent());
        progressBar.setValue(prog);
        progressBar.setString(String.format("%d%% (%d/%d words)", prog, engine.getCompletedWords(), engine.getTotalWords()));
    }

    private void endTest() {
        if (!isRunning) return;
        isRunning = false;
        isPaused = false;
        if (countdown != null) countdown.stop();
        engine.stop();

        final int actualDuration = Math.max(1, (int) Math.round(engine.elapsedSeconds()));

        TypingResult r = new TypingResult();
        r.userId = frame.getUser().id;
        r.category = (String) categoryBox.getSelectedItem();
        r.durationSeconds = actualDuration;
        r.grossWpm = engine.grossWpm();
        r.netWpm = engine.netWpm();
        r.accuracy = engine.accuracy();
        r.rawAccuracy = engine.rawAccuracy();
        r.correctChars = engine.getCorrectChars();
        r.incorrectChars = engine.getUncorrectedErrors();
        r.correctedErrors = engine.getCorrectedErrors();
        r.uncorrectedErrors = engine.getUncorrectedErrors();
        r.totalChars = engine.getTyped().length();
        r.totalKeystrokes = engine.getTotalKeystrokes();
        r.rawKeystrokes = engine.getRawKeystrokes();
        r.backspaces = engine.getBackspaces();
        r.errorPercent = engine.errorPercent();
        r.consistency = engine.typingConsistency();

        startBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);
        restartBtn.setEnabled(true);
        durationBox.setEnabled(true);
        categoryBox.setEnabled(true);
        statusLabel.setText("Completed");

        final double previousPb = currentPbWpm;
        final var historicalErrors = engine.getHistoricalErrorsByChar();

        // Background persistence
        SwingWorker<SaveResultData, Void> worker = new SwingWorker<>() {
            @Override
            protected SaveResultData doInBackground() {
                boolean isNewPb = stats.saveResult(r, historicalErrors);
                int total = stats.testCount(r.userId);
                List<Achievement> unlocked = achievements.evaluateAfterTest(r.userId, r, total);
                unlocked.addAll(achievements.evaluateStreak(r.userId, stats.streak(r.userId)));
                if (r.consistency >= 90.0) {
                    unlocked.addAll(achievements.evaluateAfterTest(r.userId, r, total)); // evaluated
                }
                return new SaveResultData(isNewPb, unlocked);
            }

            @Override
            protected void done() {
                try {
                    SaveResultData data = get();
                    if (data.isNewPb) {
                        SoundManager.achievement();
                    } else {
                        SoundManager.complete();
                    }

                    TypingResultDialog dlg = new TypingResultDialog(
                            SwingUtilities.getWindowAncestor(TypingTestPanel.this),
                            r, data.isNewPb, previousPb, data.unlocked, TypingTestPanel.this::startTest);
                    dlg.setVisible(true);

                    frame.refreshAll();
                    updatePbLabel();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private static class SaveResultData {
        final boolean isNewPb;
        final List<Achievement> unlocked;

        SaveResultData(boolean isNewPb, List<Achievement> unlocked) {
            this.isNewPb = isNewPb;
            this.unlocked = unlocked;
        }
    }

    @Override
    public void refresh() {
        updatePbLabel();
    }
}
