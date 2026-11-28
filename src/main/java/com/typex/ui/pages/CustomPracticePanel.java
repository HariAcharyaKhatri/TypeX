package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.CustomExerciseDAO;
import com.typex.model.CustomExercise;
import com.typex.service.TypingEngine;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingTextPanel;
import com.typex.util.IconManager;
import com.typex.util.SoundManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class CustomPracticePanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final CustomExerciseDAO dao = new CustomExerciseDAO();
    private final DefaultListModel<CustomExercise> model = new DefaultListModel<>();
    private final JList<CustomExercise> list = new JList<>(model);
    private final JTextField titleField = new JTextField(18);
    private final JComboBox<String> diffBox = new JComboBox<>(new String[]{"Custom", "Easy", "Normal", "Hard", "Expert"});
    private final JTextArea textArea = new JTextArea(5, 40);
    private final JLabel countLabel = new JLabel("Chars: 0 · Words: 0");
    private final TypingTextPanel practicePane = new TypingTextPanel();
    private final JButton saveBtn = new JButton("Save");
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton newBtn = new JButton("New");
    private final JButton importBtn = new JButton("Import .txt");
    private final JButton startBtn = new JButton("Start Practice");
    private final JLabel wpmLabel = new JLabel("WPM: 0");
    private final JLabel accLabel = new JLabel("Acc: 100%");
    private final JScrollPane leftScroll;

    private TypingEngine engine;
    private CustomExercise active;
    private Timer ticker;

    public CustomPracticePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Left: exercise list
        list.setFont(ThemeManager.getBodyFont(13f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, val, index, isSelected, cellHasFocus);
                if (val instanceof CustomExercise ce) {
                    lbl.setText(ce.title + "  [" + (ce.difficulty != null ? ce.difficulty : "Custom") + "]");
                    lbl.setIcon(IconManager.getIcon("custom", 14, isSelected ? ThemeManager.fgOnAccent() : ThemeManager.accent()));
                    lbl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                }
                return lbl;
            }
        });

        leftScroll = new JScrollPane(list);
        leftScroll.setPreferredSize(new Dimension(280, 0));
        leftScroll.setBorder(BorderFactory.createTitledBorder("My Custom Tests"));
        add(leftScroll, BorderLayout.WEST);

        // Center: editor & practice pane
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        top.add(new JLabel("Title:"));
        top.add(titleField);
        top.add(new JLabel("Difficulty:"));
        top.add(diffBox);

        newBtn.setIcon(IconManager.getIcon("restart", 14, ThemeManager.fgPrimary()));
        saveBtn.setIcon(IconManager.getIcon("check", 14, ThemeManager.fgPrimary()));
        deleteBtn.setIcon(IconManager.getIcon("exit", 14, ThemeManager.colorError()));
        importBtn.setIcon(IconManager.getIcon("custom", 14, ThemeManager.fgPrimary()));

        newBtn.addActionListener(e -> clearForm());
        saveBtn.addActionListener(e -> saveCurrent());
        deleteBtn.addActionListener(e -> deleteCurrent());
        importBtn.addActionListener(e -> importTxt());

        top.add(newBtn);
        top.add(saveBtn);
        top.add(deleteBtn);
        top.add(importBtn);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(ThemeManager.getMonoFont(14f));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateCount(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateCount(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateCount(); }
        });

        JScrollPane editorScroll = new JScrollPane(textArea);
        editorScroll.setPreferredSize(new Dimension(0, 110));
        editorScroll.setBorder(BorderFactory.createTitledBorder("Test Content"));

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        countRow.setOpaque(false);
        countLabel.setFont(ThemeManager.getMonoFont(11f));
        countLabel.setForeground(ThemeManager.fgMuted());
        countRow.add(countLabel);

        JPanel editorPanel = new JPanel(new BorderLayout(2, 2));
        editorPanel.setOpaque(false);
        editorPanel.add(top, BorderLayout.NORTH);
        editorPanel.add(editorScroll, BorderLayout.CENTER);
        editorPanel.add(countRow, BorderLayout.SOUTH);

        center.add(editorPanel, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setOpaque(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        buttons.setOpaque(false);
        wpmLabel.setFont(ThemeManager.getMonoFont(16f));
        accLabel.setFont(ThemeManager.getMonoFont(16f));
        buttons.add(wpmLabel);
        buttons.add(accLabel);

        startBtn.setFocusPainted(false);
        startBtn.setIcon(IconManager.getIcon("play", 15, ThemeManager.fgOnAccent()));
        startBtn.addActionListener(e -> startPractice());
        buttons.add(startBtn);
        bottom.add(buttons, BorderLayout.NORTH);

        JScrollPane practiceScroll = new JScrollPane(practicePane);
        practiceScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        bottom.add(practiceScroll, BorderLayout.CENTER);

        center.add(bottom, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Key & focus handling for practice pane
        practicePane.setFocusable(true);
        practicePane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                practicePane.requestFocusInWindow();
            }
        });

        practicePane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (engine == null || engine.isComplete()) return;
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    engine.backspace();
                    e.consume();
                    practicePane.render();
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
                practicePane.render();
                updateMetrics();
                if (engine.isComplete()) {
                    if (ticker != null) ticker.stop();
                    engine.stop();
                    SoundManager.complete();
                    JOptionPane.showMessageDialog(CustomPracticePanel.this,
                            String.format("Custom Test Complete!\nWPM: %.1f\nAccuracy: %.1f%%", engine.netWpm(), engine.accuracy()),
                            "Test Finished", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                CustomExercise ex = list.getSelectedValue();
                active = ex;
                titleField.setText(ex.title);
                textArea.setText(ex.text);
                diffBox.setSelectedItem(ex.difficulty != null ? ex.difficulty : "Custom");
                updateCount();
            }
        });

        applyTheme();
        refresh();
    }

    private void updateCount() {
        String txt = textArea.getText();
        int chars = txt.length();
        int words = txt.isBlank() ? 0 : txt.trim().split("\\s+").length;
        countLabel.setText(String.format("Chars: %,d · Words: %,d", chars, words));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        list.setBackground(ThemeManager.bgCard());
        list.setForeground(ThemeManager.fgPrimary());
        textArea.setBackground(ThemeManager.bgCard());
        textArea.setForeground(ThemeManager.fgPrimary());
        textArea.setCaretColor(ThemeManager.accent());
        titleField.setBackground(ThemeManager.bgCard());
        titleField.setForeground(ThemeManager.fgPrimary());
        titleField.setCaretColor(ThemeManager.accent());

        newBtn.setBackground(ThemeManager.bgCard());
        newBtn.setForeground(ThemeManager.fgPrimary());
        saveBtn.setBackground(ThemeManager.bgCard());
        saveBtn.setForeground(ThemeManager.fgPrimary());
        deleteBtn.setBackground(ThemeManager.bgCard());
        deleteBtn.setForeground(ThemeManager.colorError());
        importBtn.setBackground(ThemeManager.bgCard());
        importBtn.setForeground(ThemeManager.fgPrimary());

        startBtn.setBackground(ThemeManager.accent());
        startBtn.setForeground(ThemeManager.fgOnAccent());
        wpmLabel.setForeground(ThemeManager.accent());
        accLabel.setForeground(ThemeManager.accent());

        practicePane.applyTheme();
        repaint();
    }

    private void clearForm() {
        active = null;
        titleField.setText("");
        textArea.setText("");
        diffBox.setSelectedIndex(0);
        list.clearSelection();
        updateCount();
        titleField.requestFocusInWindow();
    }

    private void saveCurrent() {
        String title = titleField.getText().trim();
        String text = textArea.getText().trim();
        if (title.isEmpty() || text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and text content are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int uid = frame.getUser() != null ? frame.getUser().id : 1;
        final CustomExercise toSave = (active != null) ? active : new CustomExercise();
        toSave.userId = uid;
        toSave.title = title;
        toSave.text = text;
        toSave.difficulty = (String) diffBox.getSelectedItem();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (toSave.id == 0) {
                    dao.insert(toSave);
                } else {
                    dao.update(toSave);
                }
                return null;
            }

            @Override
            protected void done() {
                active = toSave;
                refresh();
            }
        };
        worker.execute();
    }

    private void deleteCurrent() {
        if (active == null) return;
        int conf = JOptionPane.showConfirmDialog(this,
                "Delete custom test \"" + active.title + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        final int id = active.id;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dao.delete(id);
                return null;
            }

            @Override
            protected void done() {
                clearForm();
                refresh();
            }
        };
        worker.execute();
    }

    private void importTxt() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.length() > 500_000) {
                JOptionPane.showMessageDialog(this, "File is too large (maximum 500KB allowed for custom exercises).", "File Too Large", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String txt = Files.readString(f.toPath());
                if (txt.length() > 25000) {
                    txt = txt.substring(0, 25000);
                }
                textArea.setText(txt);
                if (titleField.getText().isBlank()) {
                    String name = f.getName();
                    if (name.toLowerCase().endsWith(".txt")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    titleField.setText(name);
                }
                updateCount();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startPractice() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select test text to practice.", "No Text", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        engine = new TypingEngine(text);
        practicePane.setEngine(engine);
        updateMetrics();

        if (ticker != null) ticker.stop();
        ticker = new Timer(300, e -> updateMetrics());
        ticker.start();

        SwingUtilities.invokeLater(practicePane::requestFocusInWindow);
    }

    private void updateMetrics() {
        if (engine == null) return;
        wpmLabel.setText(String.format("WPM: %.0f", engine.netWpm()));
        accLabel.setText(String.format("Acc: %.0f%%", engine.accuracy()));
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<List<CustomExercise>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CustomExercise> doInBackground() {
                return dao.findByUser(uid);
            }

            @Override
            protected void done() {
                try {
                    List<CustomExercise> exercises = get();
                    CustomExercise prev = active;
                    model.clear();
                    for (CustomExercise ex : exercises) {
                        model.addElement(ex);
                    }
                    if (prev != null) {
                        for (int i = 0; i < model.size(); i++) {
                            if (model.get(i).id == prev.id) {
                                list.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
