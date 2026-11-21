package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.UserPreferenceDAO;
import com.typex.model.Achievement;
import com.typex.model.PersonalBest;
import com.typex.model.TypingResult;
import com.typex.model.UserPreference;
import com.typex.service.AchievementService;
import com.typex.service.GamificationService;
import com.typex.service.StatisticsService;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingResultDialog;
import com.typex.ui.components.TypingTextPanel;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.KeyboardLayout;
import com.typex.util.SoundManager;
import com.typex.util.TextGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TypingTestPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private static final int[] DURATIONS = {15, 30, 60, 120, 180, 300};
    private static final String[] CATEGORIES = TextGenerator.CATEGORIES;

    private final MainFrame frame;
    private final StatisticsService stats = new StatisticsService();
    private final AchievementService achievements = new AchievementService();
    private final GamificationService gamification = new GamificationService();
    private final UserPreferenceDAO prefDao = new UserPreferenceDAO();

    // CardLayout switching between Config and Distraction-Free Active Typing
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // --- CONFIG VIEW COMPONENTS ---
    private final JPanel configPanel = new JPanel();
    private final List<JButton> durationChips = new ArrayList<>();
    private final JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
    private final JComboBox<String> layoutBox = new JComboBox<>(KeyboardLayout.LAYOUT_NAMES);
    private final JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(26, 18, 38, 2));
    private final JLabel pbWpmLabel = new JLabel("Best: -- WPM");
    private final JLabel pbAccLabel = new JLabel("Accuracy: --%");
    private final JLabel detectedLocaleLabel = new JLabel();
    private final JButton startBtn = new JButton("Start Typing Test  ➔");

    // --- TYPING VIEW (DISTRACTION-FREE) COMPONENTS ---
    private final JPanel typingPanel = new JPanel(new BorderLayout(0, 10));
    private final TypingTextPanel textPane = new TypingTextPanel();
    private final VirtualKeyboard virtualKeyboard = new VirtualKeyboard();

    private final JLabel timeLabel = new JLabel("60s");
    private final JLabel wpmLabel = new JLabel("0 WPM");
    private final JLabel accLabel = new JLabel("100% ACC");
    private final JLabel mistakesLabel = new JLabel("0 Mistakes");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final JButton pauseResumeBtn = new JButton("Pause");
    private final JButton restartBtn = new JButton("Restart");
    private final JButton exitBtn = new JButton("✕ Exit Test");
    private final JButton heatmapBtn = new JButton("Heatmap: Off");

    // State
    private TypingEngine engine;
    private Timer countdown;
    private int selectedDuration = 60;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private double currentPbWpm = 0.0;
    private String currentLayoutName = "US QWERTY";

    public TypingTestPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setOpaque(false);

        buildConfigView();
        buildTypingView();

        cardsPanel.setOpaque(false);
        cardsPanel.add(configPanel, "CONFIG");
        cardsPanel.add(typingPanel, "TYPING");
        add(cardsPanel, BorderLayout.CENTER);

        applyTheme();
        cardLayout.show(cardsPanel, "CONFIG");
    }

    private void buildConfigView() {
        configPanel.setLayout(new GridBagLayout());
        configPanel.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.bgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)));
        card.setMaximumSize(new Dimension(680, 680));
        card.setPreferredSize(new Dimension(640, 560));

        // Header
        JLabel title = new JLabel("Typing Speed Test");
        title.setFont(ThemeManager.getTitleFont(22f));
        title.setForeground(ThemeManager.fgPrimary());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        JLabel subtitle = new JLabel("Choose your test settings below and test your typing precision.");
        subtitle.setFont(ThemeManager.getBodyFont(13f));
        subtitle.setForeground(ThemeManager.fgSecondary());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        // Duration selector (Chips)
        JLabel durTitle = new JLabel("TEST DURATION");
        durTitle.setFont(ThemeManager.getTitleFont(11f));
        durTitle.setForeground(ThemeManager.fgMuted());
        durTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(durTitle);
        card.add(Box.createVerticalStrut(6));

        JPanel durationRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        durationRow.setOpaque(false);
        durationRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int sec : DURATIONS) {
            JButton chip = new JButton(sec + "s");
            chip.setFont(ThemeManager.getTitleFont(13f));
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(e -> {
                selectedDuration = sec;
                updateDurationChips();
                updatePbData();
            });
            durationChips.add(chip);
            durationRow.add(chip);
        }
        card.add(durationRow);
        card.add(Box.createVerticalStrut(18));

        // Category & Font Size Row
        JPanel gridRow1 = new JPanel(new GridLayout(1, 2, 16, 0));
        gridRow1.setOpaque(false);
        gridRow1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel catCol = new JPanel();
        catCol.setLayout(new BoxLayout(catCol, BoxLayout.Y_AXIS));
        catCol.setOpaque(false);
        JLabel catLabel = new JLabel("PASSAGE CATEGORY");
        catLabel.setFont(ThemeManager.getTitleFont(11f));
        catLabel.setForeground(ThemeManager.fgMuted());
        categoryBox.setFont(ThemeManager.getBodyFont(13.5f));
        categoryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        categoryBox.addActionListener(e -> updatePbData());
        catCol.add(catLabel);
        catCol.add(Box.createVerticalStrut(6));
        catCol.add(categoryBox);
        gridRow1.add(catCol);

        JPanel fontCol = new JPanel();
        fontCol.setLayout(new BoxLayout(fontCol, BoxLayout.Y_AXIS));
        fontCol.setOpaque(false);
        JLabel fontLabel = new JLabel("PASSAGE FONT SIZE");
        fontLabel.setFont(ThemeManager.getTitleFont(11f));
        fontLabel.setForeground(ThemeManager.fgMuted());
        fontSizeSpinner.setFont(ThemeManager.getBodyFont(13.5f));
        fontSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        fontCol.add(fontLabel);
        fontCol.add(Box.createVerticalStrut(6));
        fontCol.add(fontSizeSpinner);
        gridRow1.add(fontCol);

        card.add(gridRow1);
        card.add(Box.createVerticalStrut(18));

        // Keyboard Layout Row
        JPanel layoutCol = new JPanel();
        layoutCol.setLayout(new BoxLayout(layoutCol, BoxLayout.Y_AXIS));
        layoutCol.setOpaque(false);
        layoutCol.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel layoutLabel = new JLabel("KEYBOARD LAYOUT");
        layoutLabel.setFont(ThemeManager.getTitleFont(11f));
        layoutLabel.setForeground(ThemeManager.fgMuted());
        layoutBox.setFont(ThemeManager.getBodyFont(13.5f));
        layoutBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        layoutBox.addActionListener(e -> {
            String selected = (String) layoutBox.getSelectedItem();
            if (selected != null) {
                currentLayoutName = selected;
                virtualKeyboard.setLayoutInfo(KeyboardLayout.getLayout(selected));
            }
        });

        detectedLocaleLabel.setFont(ThemeManager.getBodyFont(11.5f));
        detectedLocaleLabel.setForeground(ThemeManager.fgMuted());
        detectedLocaleLabel.setText("ℹ System Input Locale: " + KeyboardLayout.detectSystemLayout() + " (Hardware wiring cannot be read by JVM)");

        layoutCol.add(layoutLabel);
        layoutCol.add(Box.createVerticalStrut(6));
        layoutCol.add(layoutBox);
        layoutCol.add(Box.createVerticalStrut(4));
        layoutCol.add(detectedLocaleLabel);
        card.add(layoutCol);
        card.add(Box.createVerticalStrut(18));

        // Personal Best Card
        JPanel pbCard = new JPanel(new BorderLayout(8, 0));
        pbCard.setBackground(ThemeManager.accentLight());
        pbCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeManager.accent()),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        pbCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        pbCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel pbTitle = new JLabel("Personal Record for this selection:");
        pbTitle.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.STAR, 16, ThemeManager.accent()));
        pbTitle.setIconTextGap(6);
        pbTitle.setFont(ThemeManager.getTitleFont(12.5f));
        pbTitle.setForeground(ThemeManager.accent());

        JPanel pbStats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pbStats.setOpaque(false);
        pbWpmLabel.setFont(ThemeManager.getTitleFont(13f));
        pbWpmLabel.setForeground(ThemeManager.fgPrimary());
        pbAccLabel.setFont(ThemeManager.getBodyFont(13f));
        pbAccLabel.setForeground(ThemeManager.fgSecondary());
        pbStats.add(pbWpmLabel);
        pbStats.add(pbAccLabel);

        pbCard.add(pbTitle, BorderLayout.WEST);
        pbCard.add(pbStats, BorderLayout.EAST);
        card.add(pbCard);
        card.add(Box.createVerticalStrut(24));

        // Start Test Button
        startBtn.setText("Start Typing Test");
        startBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PLAY, 16, ThemeManager.fgOnAccent()));
        startBtn.setIconTextGap(8);
        startBtn.setFont(ThemeManager.getTitleFont(15f));
        startBtn.setBackground(ThemeManager.accent());
        startBtn.setForeground(ThemeManager.fgOnAccent());
        startBtn.setFocusPainted(false);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        startBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        startBtn.addActionListener(e -> startTest());
        card.add(startBtn);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        configPanel.add(card, gc);

        updateDurationChips();
    }

    private void updateDurationChips() {
        for (JButton chip : durationChips) {
            String text = chip.getText().replace("s", "");
            int sec = Integer.parseInt(text);
            boolean isSelected = (sec == selectedDuration);
            if (isSelected) {
                chip.setBackground(ThemeManager.accent());
                chip.setForeground(ThemeManager.fgOnAccent());
                chip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.accent()),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            } else {
                chip.setBackground(ThemeManager.bgSecondary());
                chip.setForeground(ThemeManager.fgPrimary());
                chip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.borderColor()),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            }
        }
    }

    private void buildTypingView() {
        typingPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));
        typingPanel.setOpaque(false);

        // Minimalist Distraction-Free Header
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 48));

        // Left: Exit button
        exitBtn.setText("Exit Test");
        exitBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.EXIT, 14, ThemeManager.fgPrimary()));
        exitBtn.setIconTextGap(6);
        exitBtn.setFont(ThemeManager.getTitleFont(13f));
        exitBtn.setFocusPainted(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.addActionListener(e -> confirmExit());
        topBar.add(exitBtn, BorderLayout.WEST);

        // Center: Live Metrics HUD
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 4));
        hud.setOpaque(false);

        timeLabel.setFont(ThemeManager.getTitleFont(24f));
        timeLabel.setForeground(ThemeManager.accent());
        timeLabel.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.TIMER, 18, ThemeManager.accent()));
        timeLabel.setIconTextGap(6);

        wpmLabel.setFont(ThemeManager.getTitleFont(20f));
        wpmLabel.setForeground(ThemeManager.fgPrimary());
        wpmLabel.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.SPEED, 16, ThemeManager.fgPrimary()));
        wpmLabel.setIconTextGap(6);

        accLabel.setFont(ThemeManager.getTitleFont(20f));
        accLabel.setForeground(ThemeManager.fgPrimary());
        accLabel.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.ACCURACY, 16, ThemeManager.fgPrimary()));
        accLabel.setIconTextGap(6);

        mistakesLabel.setFont(ThemeManager.getTitleFont(18f));
        mistakesLabel.setForeground(ThemeManager.colorError());

        hud.add(timeLabel);
        hud.add(wpmLabel);
        hud.add(accLabel);
        hud.add(mistakesLabel);
        topBar.add(hud, BorderLayout.CENTER);

        // Right: Control actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        pauseResumeBtn.setText("Pause");
        pauseResumeBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PAUSE, 14, ThemeManager.fgPrimary()));
        pauseResumeBtn.setIconTextGap(6);
        pauseResumeBtn.setFont(ThemeManager.getBodyFont(13f));
        pauseResumeBtn.setFocusPainted(false);
        pauseResumeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pauseResumeBtn.addActionListener(e -> {
            if (isPaused) resumeTest(); else pauseTest();
        });

        restartBtn.setText("Restart");
        restartBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.RESTART, 14, ThemeManager.fgPrimary()));
        restartBtn.setIconTextGap(6);
        restartBtn.setFont(ThemeManager.getBodyFont(13f));
        restartBtn.setFocusPainted(false);
        restartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restartBtn.addActionListener(e -> restartTest());

        heatmapBtn.setText("Heatmap");
        heatmapBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.FLAME, 14, ThemeManager.fgSecondary()));
        heatmapBtn.setIconTextGap(6);
        heatmapBtn.setFocusPainted(false);
        heatmapBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heatmapBtn.addActionListener(e -> toggleHeatmap());

        actions.add(heatmapBtn);
        actions.add(pauseResumeBtn);
        actions.add(restartBtn);
        topBar.add(actions, BorderLayout.EAST);

        // Slim Progress Bar directly below top bar
        JPanel headerContainer = new JPanel(new BorderLayout(0, 6));
        headerContainer.setOpaque(false);
        headerContainer.add(topBar, BorderLayout.NORTH);

        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 5));
        progressBar.setForeground(ThemeManager.accent());
        headerContainer.add(progressBar, BorderLayout.SOUTH);

        typingPanel.add(headerContainer, BorderLayout.NORTH);

        // Center: Large Typing Passage
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor(), 1));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        typingPanel.add(scroll, BorderLayout.CENTER);

        // South: Full-Size Proportional Virtual Keyboard
        JPanel keyboardContainer = new JPanel(new BorderLayout());
        keyboardContainer.setOpaque(false);
        keyboardContainer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        keyboardContainer.add(virtualKeyboard, BorderLayout.CENTER);
        typingPanel.add(keyboardContainer, BorderLayout.SOUTH);

        // Key & Focus Handling
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

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmExit();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    engine.backspace();
                    e.consume();
                    textPane.render();
                    updateMetrics();
                    updateVirtualKeyboard();
                    return;
                }

                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED || c < 32) return;

                boolean ok = engine.type(c);
                if (ok) {
                    SoundManager.keypress();
                } else {
                    SoundManager.error();
                    virtualKeyboard.flashWrong(c);
                }
                textPane.render();
                updateMetrics();
                updateVirtualKeyboard();

                if (engine.isComplete()) {
                    endTest();
                }
            }
        });
    }

    private void updateVirtualKeyboard() {
        if (engine == null) return;
        int cursor = engine.getCursor();
        String original = engine.getOriginalText();
        if (cursor < original.length()) {
            char nextChar = original.charAt(cursor);
            virtualKeyboard.highlightKey(nextChar);
        } else {
            virtualKeyboard.clearHighlights();
        }
    }

    private void toggleHeatmap() {
        boolean active = !virtualKeyboard.isHeatmapMode();
        if (active && engine != null) {
            virtualKeyboard.setHeatmapMode(true, engine.getHistoricalErrorsByChar());
            heatmapBtn.setText("Heatmap: ON");
        } else {
            virtualKeyboard.setHeatmapMode(false, null);
            heatmapBtn.setText("Heatmap: OFF");
        }
    }

    private void confirmExit() {
        if (isRunning && !engine.isComplete()) {
            pauseTest();
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "A typing test is currently active.\nAre you sure you want to exit? Your progress will not be saved.",
                    "Exit Typing Test",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                abortTest();
            } else {
                resumeTest();
            }
        } else {
            abortTest();
        }
    }

    private void abortTest() {
        isRunning = false;
        isPaused = false;
        if (countdown != null) {
            countdown.stop();
        }
        if (engine != null) {
            engine.stop();
        }
        frame.setDistractionFree(false);
        cardLayout.show(cardsPanel, "CONFIG");
    }

    private void startTest() {
        if (countdown != null) {
            countdown.stop();
        }

        String cat = (String) categoryBox.getSelectedItem();
        int minChars = Math.max(300, selectedDuration * 12);
        String text = TextGenerator.generate(cat, minChars);

        int fontSize = (Integer) fontSizeSpinner.getValue();
        textPane.setFontSize(fontSize);

        // Apply active keyboard layout
        String layoutName = (String) layoutBox.getSelectedItem();
        virtualKeyboard.setLayoutInfo(KeyboardLayout.getLayout(layoutName != null ? layoutName : "US QWERTY"));

        engine = new TypingEngine(text);
        engine.start();

        textPane.setEngine(engine);
        isRunning = true;
        isPaused = false;
        pauseResumeBtn.setText("Pause");

        updateMetrics();
        updateVirtualKeyboard();

        // Switch to distraction-free active typing mode
        frame.setDistractionFree(true);
        cardLayout.show(cardsPanel, "TYPING");

        countdown = new Timer(100, e -> {
            if (!isRunning || isPaused || engine == null) return;
            double elapsed = engine.elapsedSeconds();
            int remaining = (int) Math.ceil(selectedDuration - elapsed);
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
        pauseResumeBtn.setText("Resume");
        pauseResumeBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PLAY, 14, ThemeManager.fgPrimary()));
        timeLabel.setText("[PAUSED]");
    }

    private void resumeTest() {
        if (!isRunning || !isPaused || engine == null) return;
        isPaused = false;
        engine.resume();
        if (countdown != null) countdown.start();
        pauseResumeBtn.setText("Pause");
        pauseResumeBtn.setIcon(com.typex.util.IconManager.getIcon(com.typex.util.IconManager.PAUSE, 14, ThemeManager.fgPrimary()));
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
        wpmLabel.setText(String.format("%.0f WPM", engine.netWpm()));
        accLabel.setText(String.format("%.1f%% ACC", engine.accuracy()));
        mistakesLabel.setText(engine.getUncorrectedErrors() + " Mistakes");
        int prog = (int) Math.round(engine.progressPercent());
        progressBar.setValue(prog);
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

        final double previousPb = currentPbWpm;
        final var historicalErrors = engine.getHistoricalErrorsByChar();

        // Restore non-distraction UI when showing results
        frame.setDistractionFree(false);

        SwingWorker<SaveResultData, Void> worker = new SwingWorker<>() {
            @Override
            protected SaveResultData doInBackground() {
                boolean isNewPb = stats.saveResult(r, historicalErrors);
                GamificationService.SessionReward reward = gamification.recordTestCompletion(r.userId, r, isNewPb);
                return new SaveResultData(isNewPb, reward);
            }

            @Override
            protected void done() {
                try {
                    SaveResultData data = get();
                    if (data.reward != null && data.reward.leveledUp) {
                        SoundManager.levelUp();
                    } else if (data.isNewPb) {
                        SoundManager.achievement();
                    } else {
                        SoundManager.complete();
                    }

                    TypingResultDialog dlg = new TypingResultDialog(
                            SwingUtilities.getWindowAncestor(TypingTestPanel.this),
                            r, data.isNewPb, previousPb, data.reward,
                            (data.reward != null ? data.reward.newlyUnlocked : null),
                            TypingTestPanel.this::startTest,
                            () -> {
                                cardLayout.show(cardsPanel, "CONFIG");
                                updatePbData();
                            });
                    dlg.setVisible(true);

                    frame.refreshAll();
                    updatePbData();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void updatePbData() {
        if (frame.getUser() == null) return;
        String cat = (String) categoryBox.getSelectedItem();
        final int dur = selectedDuration;

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
                        pbWpmLabel.setText(String.format("Best: %.1f WPM", pb.bestWpm));
                        pbAccLabel.setText(String.format("Accuracy: %.1f%%", pb.accuracy));
                    } else {
                        currentPbWpm = 0.0;
                        pbWpmLabel.setText("Best: -- WPM");
                        pbAccLabel.setText("Accuracy: --%");
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());

        // Buttons
        startBtn.setBackground(ThemeManager.accent());
        startBtn.setForeground(ThemeManager.fgOnAccent());

        exitBtn.setBackground(ThemeManager.bgCard());
        exitBtn.setForeground(ThemeManager.fgPrimary());
        exitBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        pauseResumeBtn.setBackground(ThemeManager.bgCard());
        pauseResumeBtn.setForeground(ThemeManager.fgPrimary());
        pauseResumeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        restartBtn.setBackground(ThemeManager.bgCard());
        restartBtn.setForeground(ThemeManager.fgPrimary());
        restartBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        heatmapBtn.setBackground(ThemeManager.bgCard());
        heatmapBtn.setForeground(ThemeManager.fgSecondary());
        heatmapBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        timeLabel.setForeground(ThemeManager.accent());
        wpmLabel.setForeground(ThemeManager.fgPrimary());
        accLabel.setForeground(ThemeManager.fgPrimary());
        mistakesLabel.setForeground(ThemeManager.colorError());
        progressBar.setForeground(ThemeManager.accent());

        updateDurationChips();
        textPane.applyTheme();
        virtualKeyboard.applyTheme();
        repaint();
    }

    @Override
    public void refresh() {
        if (frame.getUser() != null) {
            SwingWorker<UserPreference, Void> worker = new SwingWorker<>() {
                @Override
                protected UserPreference doInBackground() {
                    return prefDao.getForUser(frame.getUser().id);
                }

                @Override
                protected void done() {
                    try {
                        UserPreference p = get();
                        if (p != null) {
                            if (p.keyboardLayout != null && !p.keyboardLayout.isEmpty()) {
                                layoutBox.setSelectedItem(p.keyboardLayout);
                            }
                            if (p.passageFontSize >= 18 && p.passageFontSize <= 38) {
                                fontSizeSpinner.setValue(p.passageFontSize);
                            }
                            if (p.preferredDuration > 0) {
                                selectedDuration = p.preferredDuration;
                                updateDurationChips();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            };
            worker.execute();
        }
        updatePbData();
    }

    private static class SaveResultData {
        final boolean isNewPb;
        final GamificationService.SessionReward reward;

        SaveResultData(boolean isNewPb, GamificationService.SessionReward reward) {
            this.isNewPb = isNewPb;
            this.reward = reward;
        }
    }
}
