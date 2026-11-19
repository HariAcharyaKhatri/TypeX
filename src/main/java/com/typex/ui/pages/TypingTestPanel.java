package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.Achievement;
import com.typex.model.TypingResult;
import com.typex.service.AchievementService;
import com.typex.service.StatisticsService;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingTextPanel;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class TypingTestPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private static final String[] DURATIONS = {"15", "30", "60", "120", "180", "300"};
    private static final String[] CATEGORIES = {
            "Random Words", "Common English", "Sentences", "Quotes", "Business English", "Programming"
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
    private final JLabel statusLabel = new JLabel("Ready");

    private final JButton startBtn = new JButton("Start Test");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton resumeBtn = new JButton("Resume");
    private final JButton restartBtn = new JButton("Restart");

    private TypingEngine engine;
    private Timer countdown;
    private int selectedDurationSeconds = 60;
    private boolean isRunning = false;
    private boolean isPaused = false;

    public TypingTestPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top control bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        top.add(new JLabel("Duration (s):"));
        durationBox.setSelectedItem("60");
        top.add(durationBox);
        top.add(new JLabel("Category:"));
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

        top.add(statusLabel);
        add(top, BorderLayout.NORTH);

        // Center metrics & text pane
        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 4));
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
        statusLabel.setForeground(ThemeManager.fgMuted());

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
    }

    private void startTest() {
        if (countdown != null) {
            countdown.stop();
        }

        selectedDurationSeconds = Integer.parseInt((String) durationBox.getSelectedItem());
        String text = generateText((String) categoryBox.getSelectedItem());
        engine = new TypingEngine(text);
        engine.start(); // Start engine countdown immediately

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

        // Reliable countdown timer ticking every 100ms
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
        mistakesLabel.setText("Mistakes: " + engine.getIncorrectChars());
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
        r.correctChars = engine.getCorrectChars();
        r.incorrectChars = engine.getIncorrectChars();
        r.totalChars = engine.getTyped().length();
        r.totalKeystrokes = engine.getTotalKeystrokes();
        r.backspaces = engine.getBackspaces();
        r.errorPercent = engine.errorPercent();

        startBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);
        restartBtn.setEnabled(true);
        durationBox.setEnabled(true);
        categoryBox.setEnabled(true);
        statusLabel.setText("Completed");

        // Save in background to not block EDT
        SwingWorker<List<Achievement>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Achievement> doInBackground() {
                stats.saveResult(r);
                int total = stats.testCount(r.userId);
                List<Achievement> unlocked = achievements.evaluateAfterTest(r.userId, r, total);
                unlocked.addAll(achievements.evaluateStreak(r.userId, stats.streak(r.userId)));
                return unlocked;
            }

            @Override
            protected void done() {
                try {
                    List<Achievement> unlocked = get();
                    StringBuilder sb = new StringBuilder(String.format(
                            "Test complete!\n\nDuration: %d seconds\nNet WPM: %.1f\nGross WPM: %.1f\nAccuracy: %.1f%%\n" +
                            "Correct Characters: %d\nMistakes: %d\nBackspaces: %d\n",
                            actualDuration, r.netWpm, r.grossWpm, r.accuracy, r.correctChars, r.incorrectChars, r.backspaces));
                    if (!unlocked.isEmpty()) {
                        sb.append("\nAchievements Unlocked:\n");
                        for (Achievement a : unlocked) {
                            sb.append("🏆 ").append(a.title).append(" (+").append(a.xp).append(" XP)\n");
                        }
                        SoundManager.achievement();
                    } else {
                        SoundManager.complete();
                    }
                    JOptionPane.showMessageDialog(TypingTestPanel.this, sb.toString(), "Test Results", JOptionPane.INFORMATION_MESSAGE);
                    frame.refreshAll();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private String generateText(String category) {
        String base;
        switch (category) {
            case "Sentences" -> base = "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. Sphinx of black quartz, judge my vow. ";
            case "Quotes" -> base = "The only way to do great work is to love what you do. Stay hungry, stay foolish. It does not matter how slowly you go as long as you do not stop. ";
            case "Business English" -> base = "Please review the attached quarterly report and submit your feedback by Friday. Our team strives to maximize efficiency and client satisfaction across all deliverables. ";
            case "Programming" -> base = "public static void main(String[] args) { int count = 0; if (count >= 0) return true; List<String> items = new ArrayList<>(); } ";
            case "Random Words" -> base = "alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo lima mike november oscar papa quebec romeo sierra tango uniform victor whiskey xray yankee zulu. ";
            default -> base = "the and for you that with this have from they which one were her all she there would their we him been has when who will more no if out so said what up. ";
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 700) {
            sb.append(base);
        }
        return sb.substring(0, 700);
    }

    @Override
    public void refresh() {}
}
