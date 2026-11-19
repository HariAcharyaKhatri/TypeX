package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.LessonDAO;
import com.typex.model.Lesson;
import com.typex.service.AchievementService;
import com.typex.service.LessonService;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingTextPanel;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class LearnTypingPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final LessonDAO lessonDAO = new LessonDAO();
    private final LessonService lessonService = new LessonService();
    private final AchievementService achievementService = new AchievementService();
    private final DefaultListModel<Lesson> model = new DefaultListModel<>();
    private final JList<Lesson> list = new JList<>(model);
    private final TypingTextPanel textPane = new TypingTextPanel();
    private final VirtualKeyboard keyboard = new VirtualKeyboard();
    private final JLabel status = new JLabel("Select a lesson to begin");
    private final JLabel timerLabel = new JLabel("00:00");
    private final JLabel wpmLabel = new JLabel("WPM: 0");
    private final JLabel accLabel = new JLabel("Acc: 100%");
    private final JButton restart = new JButton("Restart Lesson");
    private final JScrollPane listScroll;

    private TypingEngine engine;
    private Lesson active;
    private Timer ticker;

    public LearnTypingPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Left: lesson list
        list.setFont(list.getFont().deriveFont(13f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                select(list.getSelectedValue());
            }
        });
        listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(280, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("Lessons"));
        add(listScroll, BorderLayout.WEST);

        // Center: header + text + keyboard
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        header.add(status, BorderLayout.WEST);

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

        restart.addActionListener(e -> startActive());
        restart.setFocusPainted(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(restart);
        add(south, BorderLayout.SOUTH);

        // Focus & Key handling
        textPane.setFocusable(true);
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                textPane.requestFocusInWindow();
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (engine == null || engine.isComplete()) return;
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
        status.setForeground(ThemeManager.fgPrimary());
        timerLabel.setForeground(ThemeManager.accent());
        wpmLabel.setForeground(ThemeManager.accent());
        accLabel.setForeground(ThemeManager.accent());
        restart.setBackground(ThemeManager.accent());
        restart.setForeground(Color.BLACK);
        textPane.applyTheme();
        keyboard.applyTheme();
        repaint();
    }

    private void select(Lesson l) {
        if (l == null) return;
        active = l;
        status.setText(l.title + " — " + (l.objective == null ? "" : l.objective));
        startActive();
    }

    private void startActive() {
        if (active == null) return;
        engine = new TypingEngine(active.practiceText);
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
        if (engine == null || active == null || active.practiceText == null) return;
        int cur = engine.getCursor();
        if (cur < active.practiceText.length()) {
            keyboard.highlightKey(active.practiceText.charAt(cur));
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
        final int uid = frame.getUser().id;
        final Lesson finishedLesson = active;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                lessonService.recordAttempt(uid, finishedLesson, wpm, acc);
                int completed = lessonService.completedCount(uid);
                var newly = achievementService.evaluateAfterLesson(uid, completed);
                if (!newly.isEmpty()) {
                    SoundManager.achievement();
                } else {
                    SoundManager.complete();
                }
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(LearnTypingPanel.this,
                        String.format("Lesson complete!\nWPM: %.1f\nAccuracy: %.1f%%", wpm, acc),
                        "Lesson Finished", JOptionPane.INFORMATION_MESSAGE);
                frame.refreshAll();
            }
        };
        worker.execute();
    }

    @Override
    public void refresh() {
        SwingWorker<List<Lesson>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Lesson> doInBackground() {
                return lessonDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Lesson> lessons = get();
                    Lesson previouslySelected = active;
                    model.clear();
                    for (Lesson l : lessons) {
                        model.addElement(l);
                    }
                    if (previouslySelected != null) {
                        for (int i = 0; i < model.size(); i++) {
                            if (model.get(i).id == previouslySelected.id) {
                                list.setSelectedIndex(i);
                                break;
                            }
                        }
                    } else if (!model.isEmpty()) {
                        list.setSelectedIndex(0);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
