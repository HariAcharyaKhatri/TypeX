package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.UserPreferenceDAO;
import com.typex.model.UserPreference;
import com.typex.ui.MainFrame;
import com.typex.util.KeyboardLayout;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class SettingsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsPanel.class);
    private final UserPreferenceDAO prefDao = new UserPreferenceDAO();

    private final JCheckBox darkTheme = new JCheckBox("Dark Theme");
    private final JCheckBox sound = new JCheckBox("Sound Effects");
    private final JSlider volume = new JSlider(0, 100, 70);
    private final JComboBox<String> accentBox = new JComboBox<>(new String[]{"Classic Blue", "Gold", "Emerald", "Rose"});
    private final JSpinner passageFontSize = new JSpinner(new SpinnerNumberModel(26, 18, 38, 2));
    private final JComboBox<String> keyboardLayout = new JComboBox<>(KeyboardLayout.LAYOUT_NAMES);
    private final JLabel detectedLayoutLabel = new JLabel();

    private final JComboBox<Integer> defaultDuration = new JComboBox<>(new Integer[]{15, 30, 60, 120, 180, 300});
    private final JCheckBox showKeyboard = new JCheckBox("Show Virtual Keyboard");
    private final JCheckBox fingerGuide = new JCheckBox("Show Finger Guidance");

    // Gamification & Daily Goals
    private final JSpinner goalMinutes = new JSpinner(new SpinnerNumberModel(15, 5, 180, 5));
    private final JSpinner goalTests = new JSpinner(new SpinnerNumberModel(3, 1, 30, 1));
    private final JSpinner goalLessons = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
    private final JSpinner goalAccuracy = new JSpinner(new SpinnerNumberModel(95, 70, 100, 1));
    private final JCheckBox levelUpAlerts = new JCheckBox("Show Level-Up Alerts");
    private final JCheckBox achievementAlerts = new JCheckBox("Show Achievement Celebrations");

    private final JButton applyBtn = new JButton("Save & Apply Settings");
    private final JPanel formPanel = new JPanel(new GridBagLayout());

    public SettingsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setOpaque(false);

        formPanel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        addSection("Appearance & Typography", gc, y++);
        addRow("Theme Mode:", darkTheme, gc, y++);
        addRow("Accent Color:", accentBox, gc, y++);
        addRow("Passage Font Size (18-38px):", passageFontSize, gc, y++);

        addSection("Hardware & Keyboard Layout", gc, y++);
        addRow("Keyboard Layout:", keyboardLayout, gc, y++);
        detectedLayoutLabel.setFont(ThemeManager.getBodyFont(11.5f));
        detectedLayoutLabel.setForeground(ThemeManager.fgMuted());
        detectedLayoutLabel.setText("System input locale detected: " + KeyboardLayout.detectSystemLayout() + " (Physical keycap markings cannot be read via JVM)");
        gc.gridx = 1; gc.gridy = y++;
        formPanel.add(detectedLayoutLabel, gc);

        addSection("Typing & Guidance Preferences", gc, y++);
        addRow("Default Test Duration (s):", defaultDuration, gc, y++);
        addRow("Virtual Keyboard:", showKeyboard, gc, y++);
        addRow("Finger Guidance Guide:", fingerGuide, gc, y++);

        addSection("Daily Goals & Personalization", gc, y++);
        addRow("Daily Practice Target (min):", goalMinutes, gc, y++);
        addRow("Daily Tests Target:", goalTests, gc, y++);
        addRow("Daily Lessons Target:", goalLessons, gc, y++);
        addRow("Target Accuracy (%):", goalAccuracy, gc, y++);
        addRow("Level-Up Notifications:", levelUpAlerts, gc, y++);
        addRow("Achievement Notifications:", achievementAlerts, gc, y++);

        addSection("Audio Settings", gc, y++);
        addRow("Sound Effects:", sound, gc, y++);
        addRow("Master Volume:", volume, gc, y++);

        applyBtn.setFont(ThemeManager.getTitleFont(14f));
        applyBtn.setFocusPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> apply());
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 2;
        gc.insets = new Insets(18, 10, 10, 10);
        formPanel.add(applyBtn, gc);

        // Load existing
        darkTheme.setSelected(prefs.getBoolean("theme.dark", false));
        sound.setSelected(prefs.getBoolean("sound.enabled", true));
        volume.setValue(prefs.getInt("sound.volume", 70));
        accentBox.setSelectedItem(prefs.get("theme.accent", "Classic Blue"));
        passageFontSize.setValue(prefs.getInt("font.size", 26));
        defaultDuration.setSelectedItem(prefs.getInt("typing.duration", 60));
        showKeyboard.setSelected(prefs.getBoolean("typing.keyboard", true));
        fingerGuide.setSelected(prefs.getBoolean("typing.finger", true));
        keyboardLayout.setSelectedItem(prefs.get("keyboard.layout", "US QWERTY"));

        JScrollPane sp = new JScrollPane(formPanel);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);

        applyTheme();
        refresh();
    }

    private void addSection(String title, GridBagConstraints gc, int y) {
        JLabel l = new JLabel(title);
        l.setFont(ThemeManager.getTitleFont(14f));
        l.setForeground(ThemeManager.accent());
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 2;
        gc.insets = new Insets(16, 10, 6, 10);
        formPanel.add(l, gc);
        gc.insets = new Insets(6, 10, 6, 10);
    }

    private void addRow(String label, JComponent c, GridBagConstraints gc, int y) {
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 1;
        JLabel l = new JLabel(label);
        l.setFont(ThemeManager.getBodyFont(13f));
        l.setForeground(ThemeManager.fgPrimary());
        formPanel.add(l, gc);
        gc.gridx = 1;
        formPanel.add(c, gc);
    }

    private void apply() {
        boolean dark = darkTheme.isSelected();
        String ac = (String) accentBox.getSelectedItem();
        Color accent = switch (ac != null ? ac : "Classic Blue") {
            case "Gold" -> new Color(0xF59E0B);
            case "Emerald" -> new Color(0x10B981);
            case "Rose" -> new Color(0xEC4899);
            default -> new Color(0x2563EB); // Classic Professional Blue
        };

        String selectedLayout = (String) keyboardLayout.getSelectedItem();
        int selectedFontSize = (Integer) passageFontSize.getValue();

        prefs.putBoolean("theme.dark", dark);
        prefs.put("theme.accent", ac != null ? ac : "Classic Blue");
        prefs.putInt("font.size", selectedFontSize);
        prefs.putInt("typing.duration", (Integer) defaultDuration.getSelectedItem());
        prefs.putBoolean("typing.keyboard", showKeyboard.isSelected());
        prefs.putBoolean("typing.finger", fingerGuide.isSelected());
        prefs.put("keyboard.layout", selectedLayout != null ? selectedLayout : "US QWERTY");

        SoundManager.setEnabled(sound.isSelected());
        SoundManager.setVolume(volume.getValue());

        // Save User Preferences to SQLite
        if (frame.getUser() != null) {
            UserPreference up = new UserPreference(frame.getUser().id);
            up.keyboardLayout = selectedLayout;
            up.passageFontSize = selectedFontSize;
            up.dailyGoalMinutes = (Integer) goalMinutes.getValue();
            up.dailyGoalTests = (Integer) goalTests.getValue();
            up.dailyGoalLessons = (Integer) goalLessons.getValue();
            up.dailyGoalAccuracy = ((Number) goalAccuracy.getValue()).doubleValue();
            up.preferredDuration = (Integer) defaultDuration.getSelectedItem();
            up.soundEnabled = sound.isSelected();
            up.volume = volume.getValue();
            up.levelUpAlerts = levelUpAlerts.isSelected();
            up.achievementAlerts = achievementAlerts.isSelected();
            prefDao.save(up);
        }

        ThemeManager.apply(dark, accent);
        frame.applyTheme();
        JOptionPane.showMessageDialog(this, "Settings updated successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        applyBtn.setBackground(ThemeManager.accent());
        applyBtn.setForeground(ThemeManager.fgOnAccent());
        applyBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        detectedLayoutLabel.setForeground(ThemeManager.fgMuted());
        repaint();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<UserPreference, Void> worker = new SwingWorker<>() {
            @Override
            protected UserPreference doInBackground() {
                return prefDao.getForUser(uid);
            }

            @Override
            protected void done() {
                try {
                    UserPreference p = get();
                    goalMinutes.setValue(p.dailyGoalMinutes);
                    goalTests.setValue(p.dailyGoalTests);
                    goalLessons.setValue(p.dailyGoalLessons);
                    goalAccuracy.setValue((int) Math.round(p.dailyGoalAccuracy));
                    levelUpAlerts.setSelected(p.levelUpAlerts);
                    achievementAlerts.setSelected(p.achievementAlerts);
                    if (p.keyboardLayout != null && !p.keyboardLayout.isEmpty()) {
                        keyboardLayout.setSelectedItem(p.keyboardLayout);
                    }
                    if (p.passageFontSize >= 18 && p.passageFontSize <= 38) {
                        passageFontSize.setValue(p.passageFontSize);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
