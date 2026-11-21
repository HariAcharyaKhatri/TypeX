package com.typex.ui.pages;

import com.typex.dao.UserDAO;
import com.typex.model.User;
import com.typex.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProfilePanel extends JDialog {
    private final MainFrame frame;
    private final UserDAO dao = new UserDAO();
    private final JComboBox<User> userBox = new JComboBox<>();
    private final JTextField usernameField = new JTextField(15);
    private final JComboBox<String> skillBox = new JComboBox<>(new String[]{"Beginner", "Intermediate", "Advanced", "Pro"});
    private final JSpinner goalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 500, 5));
    private final JLabel statsSummary = new JLabel("-");

    public ProfilePanel(MainFrame frame, User current) {
        super(frame, "User Profile Management", true);
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setSize(480, 370);
        setLocationRelativeTo(frame);
        setResizable(false);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        loadUsers(current);

        form.add(new JLabel("Active Profile:"));
        form.add(userBox);
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Skill Level:"));
        form.add(skillBox);
        form.add(new JLabel("Daily Goal (minutes):"));
        form.add(goalSpinner);
        form.add(new JLabel("Rank & Streak:"));
        form.add(statsSummary);

        add(form, BorderLayout.CENTER);

        // When switching user in combo box, update the fields
        userBox.addActionListener(e -> updateFieldsForSelected());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton createBtn = new JButton("Create New");
        JButton deleteBtn = new JButton("Delete");
        JButton saveBtn = new JButton("Save & Switch");
        saveBtn.setBackground(com.typex.config.ThemeManager.accent());
        saveBtn.setForeground(com.typex.config.ThemeManager.fgOnAccent());

        createBtn.addActionListener(e -> createProfile());
        deleteBtn.addActionListener(e -> deleteProfile());
        saveBtn.addActionListener(e -> saveAndSwitch());

        btns.add(createBtn);
        btns.add(deleteBtn);
        btns.add(saveBtn);
        add(btns, BorderLayout.SOUTH);
    }

    private void loadUsers(User current) {
        userBox.removeAllItems();
        List<User> list = dao.findAll();
        User toSelect = null;
        for (User u : list) {
            userBox.addItem(u);
            if (current != null && u.id == current.id) {
                toSelect = u;
            }
        }
        if (toSelect != null) {
            userBox.setSelectedItem(toSelect);
        } else if (userBox.getItemCount() > 0) {
            userBox.setSelectedIndex(0);
        }

        updateFieldsForSelected();
    }

    private void updateFieldsForSelected() {
        User sel = (User) userBox.getSelectedItem();
        if (sel != null) {
            usernameField.setText(sel.username);
            skillBox.setSelectedItem(sel.skillLevel);
            goalSpinner.setValue(sel.dailyGoal > 0 ? sel.dailyGoal : 30);
            try {
                com.typex.model.UserXP xp = new com.typex.dao.UserXPDAO().getForUser(sel.id);
                com.typex.model.PracticeStreak ps = new com.typex.dao.PracticeStreakDAO().getForUser(sel.id);
                statsSummary.setText(String.format("Level %d (%d XP) · 🔥 %d day streak", xp.level, xp.totalXp, ps.currentStreak));
            } catch (Exception e) {
                statsSummary.setText("-");
            }
        }
    }

    private void createProfile() {
        String input = JOptionPane.showInputDialog(this, "Enter username for new profile (letters, numbers, _ -):", "New Profile", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;
        String clean = input.trim();

        if (clean.length() < 2 || clean.length() > 30) {
            JOptionPane.showMessageDialog(this, "Username must be between 2 and 30 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!clean.matches("^[a-zA-Z0-9_ -]+$")) {
            JOptionPane.showMessageDialog(this, "Username can only contain letters, numbers, spaces, underscores, and hyphens.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dao.findByUsername(clean) != null) {
            JOptionPane.showMessageDialog(this, "A user with the name '" + clean + "' already exists.", "Duplicate User", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User newUser = dao.create(clean);
            loadUsers(newUser);
            JOptionPane.showMessageDialog(this, "Profile '" + clean + "' created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to create profile: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProfile() {
        User sel = (User) userBox.getSelectedItem();
        if (sel == null) return;

        boolean isActive = (frame.getUser() != null && frame.getUser().id == sel.id);
        String msg = isActive
                ? "You are about to delete the active user profile (" + sel.username + ").\nAll associated statistics and history will be permanently deleted.\n\nAre you sure you want to proceed?"
                : "Are you sure you want to delete profile '" + sel.username + "' and all its data?";

        int conf = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete Profile", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        try {
            dao.delete(sel.id);

            List<User> remaining = dao.findAll();
            User fallback;
            if (remaining.isEmpty()) {
                fallback = dao.create("Guest");
            } else {
                fallback = remaining.get(0);
            }

            if (isActive) {
                frame.setUser(fallback);
            }
            loadUsers(fallback);
            JOptionPane.showMessageDialog(this, "Profile deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete profile: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAndSwitch() {
        User sel = (User) userBox.getSelectedItem();
        if (sel == null) return;

        String name = usernameField.getText().trim();
        if (name.length() < 2 || name.length() > 30) {
            JOptionPane.showMessageDialog(this, "Username must be between 2 and 30 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!name.matches("^[a-zA-Z0-9_ -]+$")) {
            JOptionPane.showMessageDialog(this, "Username can only contain letters, numbers, spaces, underscores, and hyphens.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User existing = dao.findByUsername(name);
        if (existing != null && existing.id != sel.id) {
            JOptionPane.showMessageDialog(this, "Another user with the name '" + name + "' already exists.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            return;
        }

        sel.username = name;
        sel.skillLevel = (String) skillBox.getSelectedItem();
        sel.dailyGoal = (Integer) goalSpinner.getValue();

        try {
            dao.update(sel);
            frame.setUser(sel);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update profile: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
