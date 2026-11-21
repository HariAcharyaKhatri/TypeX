package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.LessonDAO;
import com.typex.model.Lesson;
import com.typex.model.LessonAttempt;
import com.typex.service.AchievementService;
import com.typex.service.GamificationService;
import com.typex.service.LessonService;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingTextPanel;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.IconManager;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class LearnTypingPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final LessonDAO lessonDAO = new LessonDAO();
    private final LessonService lessonService = new LessonService();
    private final AchievementService achievementService = new AchievementService();
    private final GamificationService gamificationService = new GamificationService();

    private final DefaultListModel<LessonItem> model = new DefaultListModel<>();
    private final JList<LessonItem> list = new JList<>(model);
    private final TypingTextPanel textPane = new TypingTextPanel();
    private final VirtualKeyboard keyboard = new VirtualKeyboard();

    private final JLabel titleLabel = new JLabel("Select a lesson to begin");
    private final JLabel objectiveLabel = new JLabel();
    private final JLabel requirementsLabel = new JLabel();
    private final JLabel timerLabel = new JLabel("00:00");
    private final JLabel wpmLabel = new JLabel("WPM: 0");
    private final JLabel accLabel = new JLabel("Acc: 100%");
    private final JButton restartBtn = new JButton("Start Lesson");
    private final JScrollPane listScroll;

    // History panel
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    private List<Lesson> allLessons = new ArrayList<>();
    private TypingEngine engine;
    private Lesson activeLesson;
    private boolean activeLessonUnlocked = true;
    private Timer ticker;

    public LearnTypingPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Left sidebar: lessons list & recent attempts
        list.setFont(list.getFont().deriveFont(13f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, val, index, isSelected, cellHasFocus);
                if (val instanceof LessonItem item) {
                    lbl.setText(String.format("L%d.%d: %s", item.lesson.level, item.lesson.orderIndex, item.lesson.title));
                    String iconKey = item.completed ? "check" : (item.unlocked ? "play" : "lock");
                    Color iconCol = isSelected ? ThemeManager.fgOnAccent() : (item.completed ? ThemeManager.colorSuccess() : (item.unlocked ? ThemeManager.accent() : ThemeManager.fgMuted()));
                    lbl.setIcon(IconManager.getIcon(iconKey, 14, iconCol));
                    lbl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                }
                return lbl;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                selectLesson(list.getSelectedValue().lesson);
            }
        });

        listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(280, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("Curriculum Lessons"));

        historyList.setFont(historyList.getFont().deriveFont(11f));
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(280, 140));
        historyScroll.setBorder(BorderFactory.createTitledBorder("Attempt History"));

        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.setOpaque(false);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(historyScroll, BorderLayout.SOUTH);
        add(leftPanel, BorderLayout.WEST);

        // Center: header + text + keyboard
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        objectiveLabel.setFont(objectiveLabel.getFont().deriveFont(Font.PLAIN, 12f));
        requirementsLabel.setFont(requirementsLabel.getFont().deriveFont(Font.BOLD, 12f));

        titleBlock.add(titleLabel);
        titleBlock.add(objectiveLabel);
        titleBlock.add(requirementsLabel);
        header.add(titleBlock, BorderLayout.WEST);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        metrics.setOpaque(false);
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 15f));
        wpmLabel.setFont(wpmLabel.getFont().deriveFont(Font.BOLD, 15f));
        accLabel.setFont(accLabel.getFont().deriveFont(Font.BOLD, 15f));

        metrics.add(timerLabel);
        metrics.add(wpmLabel);
        metrics.add(accLabel);
        header.add(metrics, BorderLayout.EAST);
        center.add(header, BorderLayout.NORTH);

        JScrollPane textScroll = new JScrollPane(textPane);
        textScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(0x333333) : new Color(0xDDDDDD)));
        center.add(textScroll, BorderLayout.CENTER);
        center.add(keyboard, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        restartBtn.addActionListener(e -> startActive());
        restartBtn.setFocusPainted(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(restartBtn);
        add(south, BorderLayout.SOUTH);

        // Focus & Key handling
        textPane.setFocusable(true);
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (activeLessonUnlocked) {
                    textPane.requestFocusInWindow();
                }
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (engine == null || engine.isComplete() || !activeLessonUnlocked) return;
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    engine.backspace();
                    e.consume();
                    textPane.render();
                    updateMetrics();
                    updateKeyboardHighlight();
                    return;
                }
                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED || c == '\n' || c == '\t' || c < 32) return;

                boolean ok = engine.type(c);
                if (ok) {
                    SoundManager.keypress();
                } else {
                    SoundManager.error();
                    keyboard.flashWrong(c);
                }
                textPane.render();
                updateMetrics();

                if (engine.isComplete()) {
                    finishLesson();
                } else {
                    updateKeyboardHighlight();
                }
            }
        });

        applyTheme();
        refresh();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        list.setBackground(ThemeManager.bgCard());
        list.setForeground(ThemeManager.fgPrimary());
        historyList.setBackground(ThemeManager.bgCard());
        historyList.setForeground(ThemeManager.fgPrimary());

        titleLabel.setForeground(ThemeManager.fgPrimary());
        objectiveLabel.setForeground(ThemeManager.fgMuted());
        requirementsLabel.setForeground(ThemeManager.accent());
        timerLabel.setForeground(ThemeManager.accent());
        wpmLabel.setForeground(ThemeManager.accent());
        accLabel.setForeground(ThemeManager.accent());

        restartBtn.setBackground(ThemeManager.accent());
        restartBtn.setForeground(ThemeManager.fgOnAccent());

        textPane.applyTheme();
        keyboard.applyTheme();
        repaint();
    }

    private void selectLesson(Lesson l) {
        if (l == null) return;
        activeLesson = l;
        int uid = frame.getUser().id;

        activeLessonUnlocked = lessonService.isLessonUnlocked(uid, l, allLessons);
        boolean isCompleted = lessonService.isCompleted(uid, l.id);

        titleLabel.setText((isCompleted ? "✓ " : (activeLessonUnlocked ? "▶ " : "🔒 ")) + l.title);
        objectiveLabel.setText("Objective: " + (l.objective != null ? l.objective : "-"));
        requirementsLabel.setText(String.format("Goal: ≥ %.0f WPM · ≥ %.0f%% Accuracy %s",
                l.minWpm, l.minAccuracy, (isCompleted ? "[COMPLETED]" : (activeLessonUnlocked ? "[AVAILABLE]" : "[LOCKED]"))));

        restartBtn.setEnabled(activeLessonUnlocked);
        restartBtn.setText(activeLessonUnlocked ? "Start / Restart" : "Locked");

        loadHistory(l);

        if (activeLessonUnlocked) {
            startActive();
        } else {
            if (ticker != null) ticker.stop();
            engine = null;
            textPane.setText("This lesson is locked. Complete the preceding lesson meeting both speed and accuracy goals to unlock.");
        }
    }

    private void loadHistory(Lesson l) {
        if (frame.getUser() == null || l == null) return;
        final int uid = frame.getUser().id;
        final int lid = l.id;

        SwingWorker<List<LessonAttempt>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LessonAttempt> doInBackground() {
                return lessonService.getAttemptHistory(uid, lid);
            }

            @Override
            protected void done() {
                try {
                    List<LessonAttempt> attempts = get();
                    historyModel.clear();
                    if (attempts.isEmpty()) {
                        historyModel.addElement("No attempts recorded yet.");
                    } else {
                        for (LessonAttempt a : attempts) {
                            String date = (a.attemptedAt != null && a.attemptedAt.length() > 16)
                                    ? a.attemptedAt.substring(5, 16) : a.attemptedAt;
                            String status = a.completed ? "✓ PASS" : "✗ RETRY";
                            historyModel.addElement(String.format("%s: %.0f WPM | %.0f%% [%s]",
                                    date, a.wpm, a.accuracy, status));
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void startActive() {
        if (activeLesson == null || !activeLessonUnlocked) return;
        engine = new TypingEngine(activeLesson.practiceText);
        textPane.setEngine(engine);
        updateMetrics();
        updateKeyboardHighlight();

        if (ticker != null) ticker.stop();
        ticker = new Timer(300, e -> {
            updateMetrics();
            if (engine != null && !engine.isComplete()) {
                engine.sampleWpm();
            }
        });
        ticker.start();
        SwingUtilities.invokeLater(textPane::requestFocusInWindow);
    }

    private void updateKeyboardHighlight() {
        if (engine == null || activeLesson == null || activeLesson.practiceText == null) return;
        int cur = engine.getCursor();
        if (cur < activeLesson.practiceText.length()) {
            keyboard.highlightKey(activeLesson.practiceText.charAt(cur));
        }
    }

    private void updateMetrics() {
        if (engine == null) return;
        int s = (int) engine.elapsedSeconds();
        timerLabel.setText(String.format("%02d:%02d", s / 60, s % 60));
        wpmLabel.setText(String.format("WPM: %.0f", engine.netWpm()));
        accLabel.setText(String.format("Acc: %.0f%%", engine.accuracy()));
    }

    private void finishLesson() {
        if (ticker != null) ticker.stop();
        engine.stop();
        updateMetrics();

        final double wpm = engine.netWpm();
        final double acc = engine.accuracy();
        final int elapsedSec = Math.max(1, (int) Math.round(engine.elapsedSeconds()));
        final int uid = frame.getUser().id;
        final Lesson finishedLesson = activeLesson;
        final boolean passed = (acc >= finishedLesson.minAccuracy) && (wpm >= finishedLesson.minWpm);

        SwingWorker<GamificationService.SessionReward, Void> worker = new SwingWorker<>() {
            @Override
            protected GamificationService.SessionReward doInBackground() {
                lessonService.recordAttempt(uid, finishedLesson, wpm, acc, elapsedSec);
                GamificationService.SessionReward reward = gamificationService.recordLessonCompletion(
                        uid, finishedLesson, wpm, acc, elapsedSec, passed);
                if (reward != null && reward.leveledUp) {
                    SoundManager.levelUp();
                } else if (passed) {
                    SoundManager.complete();
                } else {
                    SoundManager.error();
                }
                return reward;
            }

            @Override
            protected void done() {
                try {
                    GamificationService.SessionReward reward = get();
                    StringBuilder message = new StringBuilder();
                    if (passed) {
                        message.append(String.format("🎉 Lesson Passed!\n\nYour Speed: %.1f WPM (Goal: ≥ %.0f)\nYour Accuracy: %.1f%% (Goal: ≥ %.0f%%)\n\nNext lesson unlocked!\n",
                                wpm, finishedLesson.minWpm, acc, finishedLesson.minAccuracy));
                    } else {
                        message.append(String.format("Lesson Attempt Recorded.\n\nYour Speed: %.1f WPM (Required: ≥ %.0f)\nYour Accuracy: %.1f%% (Required: ≥ %.0f%%)\n\nTry again to unlock the next lesson!\n",
                                wpm, finishedLesson.minWpm, acc, finishedLesson.minAccuracy));
                    }

                    if (reward != null) {
                        message.append("\nRewards: +").append(reward.xpGained).append(" XP");
                        if (reward.leveledUp) {
                            message.append("\n🌟 LEVEL UP! You reached Level ").append(reward.newLevel).append("!");
                        }
                        if (reward.goalCompleted) {
                            message.append("\n🎯 Daily Goal Achieved! (+150 Bonus XP)");
                        }
                        if (reward.currentStreak > 1) {
                            message.append("\n🔥 Practice Streak: ").append(reward.currentStreak).append(" days!");
                        }
                    }

                    JOptionPane.showMessageDialog(LearnTypingPanel.this, message.toString(),
                            passed ? "Goal Achieved" : "Keep Practicing", JOptionPane.INFORMATION_MESSAGE);

                    frame.refreshAll();
                    refresh();
                } catch (Exception ignored) {
                    refresh();
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<List<LessonItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LessonItem> doInBackground() {
                allLessons = lessonDAO.findAll();
                List<LessonItem> items = new ArrayList<>();
                for (Lesson l : allLessons) {
                    boolean unlocked = lessonService.isLessonUnlocked(uid, l, allLessons);
                    boolean completed = lessonService.isCompleted(uid, l.id);
                    items.add(new LessonItem(l, unlocked, completed));
                }
                return items;
            }

            @Override
            protected void done() {
                try {
                    List<LessonItem> items = get();
                    Lesson previouslySelected = activeLesson;
                    model.clear();
                    int selectedIdx = -1;

                    for (int i = 0; i < items.size(); i++) {
                        LessonItem it = items.get(i);
                        model.addElement(it);
                        if (previouslySelected != null && it.lesson.id == previouslySelected.id) {
                            selectedIdx = i;
                        }
                    }

                    if (selectedIdx >= 0) {
                        list.setSelectedIndex(selectedIdx);
                    } else if (!model.isEmpty()) {
                        list.setSelectedIndex(0);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private static class LessonItem {
        final Lesson lesson;
        final boolean unlocked;
        final boolean completed;

        LessonItem(Lesson lesson, boolean unlocked, boolean completed) {
            this.lesson = lesson;
            this.unlocked = unlocked;
            this.completed = completed;
        }

        @Override
        public String toString() {
            String prefix = completed ? "[Done] " : (unlocked ? "[Open] " : "[Locked] ");
            return prefix + "L" + lesson.level + "." + lesson.orderIndex + ": " + lesson.title;
        }
    }
}
