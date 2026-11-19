package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class SettingsPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsPanel.class);
    private final JCheckBox darkTheme = new JCheckBox("Dark Theme");
    private final JCheckBox sound = new JCheckBox("Sound Effects");
    private final JSlider volume = new JSlider(0, 100, 70);
    private final JComboBox<String> accentBox = new JComboBox<>(new String[]{"Gold", "Electric Blue", "Emerald", "Rose"});
    private final JSpinner fontSize = new JSpinner(new SpinnerNumberModel(16, 10, 28, 1));
    private final JComboBox<Integer> defaultDuration = new JComboBox<>(new Integer[]{15, 30, 60, 120, 180, 300});
    private final JCheckBox showKeyboard = new JCheckBox("Show Virtual Keyboard");
    private final JCheckBox fingerGuide = new JCheckBox("Show Finger Guidance");
    private final JButton applyBtn = new JButton("Apply Settings");

    public SettingsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        addSection("Appearance", gc, y++);
        addRow("Theme Mode:", darkTheme, gc, y++);
        addRow("Accent Color:", accentBox, gc, y++);
        addRow("Typing Font Size:", fontSize, gc, y++);

        addSection("Typing Preferences", gc, y++);
        addRow("Default Test Duration (s):", defaultDuration, gc, y++);
        addRow("Virtual Keyboard:", showKeyboard, gc, y++);
        addRow("Finger Guidance Guide:", fingerGuide, gc, y++);

        addSection("Audio Settings", gc, y++);
        addRow("Sound Effects:", sound, gc, y++);
        addRow("Master Volume:", volume, gc, y++);

        applyBtn.setFocusPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> apply());
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 2;
        gc.insets = new Insets(16, 8, 8, 8);
        add(applyBtn, gc);

        // Load existing
        darkTheme.setSelected(prefs.getBoolean("theme.dark", true));
        sound.setSelected(prefs.getBoolean("sound.enabled", true));
        volume.setValue(prefs.getInt("sound.volume", 70));
        accentBox.setSelectedItem(prefs.get("theme.accent", "Gold"));
        fontSize.setValue(prefs.getInt("font.size", 16));
        defaultDuration.setSelectedItem(prefs.getInt("typing.duration", 60));
        showKeyboard.setSelected(prefs.getBoolean("typing.keyboard", true));
        fingerGuide.setSelected(prefs.getBoolean("typing.finger", true));

        applyTheme();
    }

    private void addSection(String title, GridBagConstraints gc, int y) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        l.setForeground(ThemeManager.accent());
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 2;
        add(l, gc);
    }

    private void addRow(String label, JComponent c, GridBagConstraints gc, int y) {
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 1;
        JLabel l = new JLabel(label);
        l.setForeground(ThemeManager.fgPrimary());
        add(l, gc);
        gc.gridx = 1;
        add(c, gc);
    }

    private void apply() {
        boolean dark = darkTheme.isSelected();
        String ac = (String) accentBox.getSelectedItem();
        Color accent = switch (ac) {
            case "Electric Blue" -> new Color(0x38BDF8);
            case "Emerald" -> new Color(0x34D399);
            case "Rose" -> new Color(0xFB7185);
            default -> new Color(0xFFC107);
        };
        prefs.putBoolean("theme.dark", dark);
        prefs.put("theme.accent", ac);
        prefs.putInt("font.size", (Integer) fontSize.getValue());
        prefs.putInt("typing.duration", (Integer) defaultDuration.getSelectedItem());
        prefs.putBoolean("typing.keyboard", showKeyboard.isSelected());
        prefs.putBoolean("typing.finger", fingerGuide.isSelected());

        SoundManager.setEnabled(sound.isSelected());
        SoundManager.setVolume(volume.getValue());

        ThemeManager.apply(dark, accent);
        frame.applyTheme();
        JOptionPane.showMessageDialog(this, "Settings updated successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        applyBtn.setBackground(ThemeManager.accent());
        applyBtn.setForeground(Color.BLACK);
        repaint();
    }

    @Override
    public void refresh() {}
}
