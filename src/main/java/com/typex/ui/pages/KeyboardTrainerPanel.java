package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.CharacterErrorDAO;
import com.typex.dao.KeyboardSessionDAO;
import com.typex.model.CharacterError;
import com.typex.model.KeyboardSession;
import com.typex.ui.MainFrame;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.IconManager;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class KeyboardTrainerPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private static final String[] DRILLS = {
            "Random Keys",
            "Home Row (ASDF JKL;)",
            "Top Row (QWERTYUIOP)",
            "Bottom Row (ZXCVBNM)",
            "Left Hand",
            "Right Hand",
            "Index Fingers",
            "Middle Fingers",
            "Ring Fingers",
            "Pinky Fingers",
            "Weak Keys Drill"
    };

    private final MainFrame frame;
    private final CharacterErrorDAO charErrorDAO = new CharacterErrorDAO();
    private final KeyboardSessionDAO sessionDAO = new KeyboardSessionDAO();

    private final VirtualKeyboard keyboard = new VirtualKeyboard();
    private final JComboBox<String> drillSelector = new JComboBox<>(DRILLS);
    private final JComboBox<String> layoutSelector = new JComboBox<>(com.typex.util.KeyboardLayout.LAYOUT_NAMES);
    private final JLabel promptLabel = new JLabel("Press Key: -");
    private final JLabel fingerInstruction = new JLabel("Finger: -");
    private final JLabel scoreLabel = new JLabel("Accuracy: 100%");
    private final JLabel streakLabel = new JLabel("Streak: 0");
    private final JLabel reactionTimeLabel = new JLabel("Reaction: 0 ms");
    private final JToggleButton heatmapToggle = new JToggleButton("Error Heatmap");

    private char currentTargetKey = 'a';
    private long keyPresentedNanos = 0;
    private int correctCount = 0;
    private int totalAttempts = 0;
    private int streak = 0;
    private int bestStreak = 0;
    private final List<Double> reactionTimesMs = new ArrayList<>();
    private final Random rng = new Random();
    private String currentPool = "asdfjkl;ghqwertyuiopzxcvbnm";

    public KeyboardTrainerPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        top.setOpaque(false);
        top.add(new JLabel("Drill Mode:"));
        drillSelector.addActionListener(e -> updateDrillPool());
        top.add(drillSelector);

        top.add(new JLabel("Layout:"));
        layoutSelector.addActionListener(e -> {
            String lName = (String) layoutSelector.getSelectedItem();
            if (lName != null) {
                keyboard.setLayoutInfo(com.typex.util.KeyboardLayout.getLayout(lName));
                nextTargetKey();
                requestFocusInWindow();
            }
        });
        top.add(layoutSelector);

        heatmapToggle.setFocusPainted(false);
        heatmapToggle.setIcon(IconManager.getIcon("flame", 14, ThemeManager.colorWarning()));
        heatmapToggle.addActionListener(e -> toggleHeatmap());
        top.add(heatmapToggle);
        add(top, BorderLayout.NORTH);

        // Center: Stats header & keyboard
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        promptLabel.setFont(promptLabel.getFont().deriveFont(Font.BOLD, 26f));
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fingerInstruction.setFont(fingerInstruction.getFont().deriveFont(Font.BOLD, 15f));
        fingerInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        statsRow.setOpaque(false);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 15f));
        streakLabel.setFont(streakLabel.getFont().deriveFont(Font.BOLD, 15f));
        reactionTimeLabel.setFont(reactionTimeLabel.getFont().deriveFont(Font.BOLD, 15f));

        statsRow.add(scoreLabel);
        statsRow.add(streakLabel);
        statsRow.add(reactionTimeLabel);

        infoPanel.add(promptLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(fingerInstruction);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(statsRow);

        center.add(infoPanel, BorderLayout.NORTH);
        center.add(keyboard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Focus & Key handling
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED || c < 32) return;
                handleKeyInput(c);
            }
        });

        applyTheme();
        updateDrillPool();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void toggleHeatmap() {
        if (heatmapToggle.isSelected()) {
            heatmapToggle.setText("⌨ Finger Guide");
            if (frame.getUser() != null) {
                SwingWorker<Map<Character, Integer>, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Map<Character, Integer> doInBackground() {
                        return charErrorDAO.getErrorCountMap(frame.getUser().id);
                    }

                    @Override
                    protected void done() {
                        try {
                            keyboard.setHeatmapMode(true, get());
                        } catch (Exception ignored) {}
                    }
                };
                worker.execute();
            }
        } else {
            heatmapToggle.setText("🔥 Error Heatmap");
            keyboard.setHeatmapMode(false, null);
        }
        requestFocusInWindow();
    }

    private void updateDrillPool() {
        String sel = (String) drillSelector.getSelectedItem();
        switch (sel != null ? sel : "Random Keys") {
            case "Home Row (ASDF JKL;)" -> currentPool = "asdfjkl;";
            case "Top Row (QWERTYUIOP)" -> currentPool = "qwertyuiop";
            case "Bottom Row (ZXCVBNM)" -> currentPool = "zxcvbnm";
            case "Left Hand" -> currentPool = "qwert" + "asdfg" + "zxcvb";
            case "Right Hand" -> currentPool = "yuiop" + "hjkl" + "nm";
            case "Index Fingers" -> currentPool = "rtyufghvbn";
            case "Middle Fingers" -> currentPool = "edcik";
            case "Ring Fingers" -> currentPool = "wsxol";
            case "Pinky Fingers" -> currentPool = "qazp";
            case "Weak Keys Drill" -> {
                loadWeakKeysPool();
                return;
            }
            default -> currentPool = "abcdefghijklmnopqrstuvwxyz";
        }
        resetStats();
        nextTargetKey();
    }

    private void loadWeakKeysPool() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<List<CharacterError>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CharacterError> doInBackground() {
                return charErrorDAO.getTopErrors(uid, 10);
            }

            @Override
            protected void done() {
                try {
                    List<CharacterError> errs = get();
                    if (errs.isEmpty()) {
                        currentPool = "asdfjkl;";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (CharacterError ce : errs) {
                            char c = (char) ce.charCode;
                            if (Character.isLetterOrDigit(c)) {
                                sb.append(Character.toLowerCase(c));
                            }
                        }
                        currentPool = sb.length() > 0 ? sb.toString() : "asdfjkl;";
                    }
                    resetStats();
                    nextTargetKey();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void resetStats() {
        correctCount = 0;
        totalAttempts = 0;
        streak = 0;
        reactionTimesMs.clear();
        updateLabels();
    }

    private void nextTargetKey() {
        if (currentPool.isEmpty()) currentPool = "asdf";
        currentTargetKey = currentPool.charAt(rng.nextInt(currentPool.length()));
        keyboard.highlightKey(currentTargetKey);
        keyPresentedNanos = System.nanoTime();

        promptLabel.setText("Press Key: '" + Character.toUpperCase(currentTargetKey) + "'");
        fingerInstruction.setText("👉 " + keyboard.getFingerInstruction(currentTargetKey));
    }

    private void handleKeyInput(char c) {
        long deltaNanos = System.nanoTime() - keyPresentedNanos;
        double reactionMs = deltaNanos / 1_000_000.0;
        totalAttempts++;

        if (Character.toLowerCase(c) == Character.toLowerCase(currentTargetKey)) {
            correctCount++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
            reactionTimesMs.add(reactionMs);

            SoundManager.keypress();
            nextTargetKey();
        } else {
            streak = 0;
            keyboard.flashWrong(c);
            SoundManager.error();

            // Track character error in DAO
            if (frame.getUser() != null) {
                charErrorDAO.recordErrors(frame.getUser().id, Map.of(currentTargetKey, 1));
            }
        }
        updateLabels();

        // Check if session completed 30 keys to persist session summary (Module 6)
        if (totalAttempts > 0 && totalAttempts % 30 == 0 && frame.getUser() != null) {
            persistSession();
        }
    }

    private void persistSession() {
        final int uid = frame.getUser().id;
        final String drill = (String) drillSelector.getSelectedItem();
        final int tested = totalAttempts;
        final int correct = correctCount;
        final double acc = (correct * 100.0) / tested;
        final double avgReact = reactionTimesMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                KeyboardSession ks = new KeyboardSession();
                ks.userId = uid;
                ks.drillType = drill;
                ks.keysTested = tested;
                ks.correctKeys = correct;
                ks.accuracy = acc;
                ks.avgReactionTimeMs = avgReact;
                sessionDAO.insert(ks);

                int count = sessionDAO.findByUser(uid, 100).size();
                new com.typex.service.AchievementService().evaluateKeyboardDrills(uid, count);
                new com.typex.dao.UserXPDAO().addXP(uid, 30);
                return null;
            }
        };
        worker.execute();
    }

    private void updateLabels() {
        double acc = totalAttempts > 0 ? (correctCount * 100.0) / totalAttempts : 100.0;
        scoreLabel.setText(String.format("Accuracy: %.0f%% (%d/%d)", acc, correctCount, totalAttempts));
        streakLabel.setText("Streak: " + streak + " (Best: " + bestStreak + ")");

        double avgReact = reactionTimesMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        reactionTimeLabel.setText(String.format("Reaction: %.0f ms", avgReact));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        promptLabel.setForeground(ThemeManager.accent());
        fingerInstruction.setForeground(ThemeManager.fgPrimary());
        scoreLabel.setForeground(ThemeManager.fgPrimary());
        streakLabel.setForeground(new Color(0xF59E0B));
        reactionTimeLabel.setForeground(ThemeManager.accent());

        keyboard.applyTheme();
        repaint();
    }

    @Override
    public void refresh() {
        if (frame.getUser() != null) {
            new SwingWorker<com.typex.model.UserPreference, Void>() {
                @Override
                protected com.typex.model.UserPreference doInBackground() {
                    return new com.typex.dao.UserPreferenceDAO().getForUser(frame.getUser().id);
                }

                @Override
                protected void done() {
                    try {
                        com.typex.model.UserPreference up = get();
                        if (up != null && up.keyboardLayout != null) {
                            layoutSelector.setSelectedItem(up.keyboardLayout);
                            keyboard.setLayoutInfo(com.typex.util.KeyboardLayout.getLayout(up.keyboardLayout));
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        }
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }
}
