package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.UserPreference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPreferenceDAO {

    public UserPreference getForUser(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureRowExists(c, userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM user_preferences WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            UserPreference p = new UserPreference(userId);
                            p.dailyGoalMinutes = rs.getInt("daily_goal_minutes");
                            p.dailyGoalTests = rs.getInt("daily_goal_tests");
                            p.dailyGoalLessons = rs.getInt("daily_goal_lessons");
                            p.dailyGoalAccuracy = rs.getDouble("daily_goal_accuracy");
                            p.preferredDuration = rs.getInt("preferred_duration");
                            p.preferredCategory = rs.getString("preferred_category");
                            p.soundEnabled = rs.getInt("sound_enabled") == 1;
                            p.volume = rs.getInt("volume");
                            p.levelUpAlerts = rs.getInt("level_up_alerts") == 1;
                            p.achievementAlerts = rs.getInt("achievement_alerts") == 1;
                            try {
                                String kl = rs.getString("keyboard_layout");
                                if (kl != null && !kl.isEmpty()) p.keyboardLayout = kl;
                                int pfs = rs.getInt("passage_font_size");
                                if (pfs >= 16 && pfs <= 40) p.passageFontSize = pfs;
                            } catch (SQLException ignored) {}
                            return p;
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching user preferences: " + e.getMessage());
            }
        }
        return new UserPreference(userId);
    }

    public void save(UserPreference p) {
        if (p == null) return;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureRowExists(c, p.userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE user_preferences SET daily_goal_minutes=?, daily_goal_tests=?, " +
                        "daily_goal_lessons=?, daily_goal_accuracy=?, preferred_duration=?, " +
                        "preferred_category=?, sound_enabled=?, volume=?, level_up_alerts=?, achievement_alerts=?, " +
                        "keyboard_layout=?, passage_font_size=? " +
                        "WHERE user_id=?")) {
                    ps.setInt(1, p.dailyGoalMinutes);
                    ps.setInt(2, p.dailyGoalTests);
                    ps.setInt(3, p.dailyGoalLessons);
                    ps.setDouble(4, p.dailyGoalAccuracy);
                    ps.setInt(5, p.preferredDuration);
                    ps.setString(6, p.preferredCategory != null ? p.preferredCategory : "Common English");
                    ps.setInt(7, p.soundEnabled ? 1 : 0);
                    ps.setInt(8, p.volume);
                    ps.setInt(9, p.levelUpAlerts ? 1 : 0);
                    ps.setInt(10, p.achievementAlerts ? 1 : 0);
                    ps.setString(11, p.keyboardLayout != null ? p.keyboardLayout : "US QWERTY");
                    ps.setInt(12, p.passageFontSize >= 16 ? p.passageFontSize : 26);
                    ps.setInt(13, p.userId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Error saving user preferences: " + e.getMessage());
            }
        }
    }

    public void updateKeyboardLayout(int userId, String layout) {
        if (layout == null) return;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureRowExists(c, userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE user_preferences SET keyboard_layout=? WHERE user_id=?")) {
                    ps.setString(1, layout);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Error updating user keyboard layout: " + e.getMessage());
            }
        }
    }

    private void ensureRowExists(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO user_preferences(user_id) VALUES(?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
