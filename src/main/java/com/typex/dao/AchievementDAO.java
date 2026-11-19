package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.Achievement;

import java.sql.*;
import java.util.*;

public class AchievementDAO {
    public List<Achievement> allForUser(int userId) {
        List<Achievement> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT a.*, CASE WHEN ua.id IS NULL THEN 0 ELSE 1 END AS unlocked " +
                    "FROM achievements a LEFT JOIN user_achievements ua " +
                    "ON a.id = ua.achievement_id AND ua.user_id = ? ORDER BY a.id")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Achievement a = new Achievement();
                        a.id = rs.getInt("id");
                        a.code = rs.getString("code");
                        a.title = rs.getString("title");
                        a.description = rs.getString("description");
                        a.icon = rs.getString("icon");
                        a.xp = rs.getInt("xp");
                        a.unlocked = rs.getInt("unlocked") == 1;
                        try {
                            String cat = rs.getString("category");
                            if (cat != null && !cat.isEmpty()) a.category = cat;
                        } catch (SQLException ignored) {}
                        out.add(a);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching achievements: " + e.getMessage());
            }
        }
        return out;
    }

    public boolean unlock(int userId, String code) {
        if (code == null) return false;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO user_achievements(user_id, achievement_id) " +
                    "SELECT ?, id FROM achievements WHERE code=?")) {
                ps.setInt(1, userId);
                ps.setString(2, code);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Error unlocking achievement: " + e.getMessage());
                return false;
            }
        }
    }

    public Achievement findByCode(String code) {
        if (code == null) return null;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM achievements WHERE code=?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    Achievement a = new Achievement();
                    a.id = rs.getInt("id");
                    a.code = rs.getString("code");
                    a.title = rs.getString("title");
                    a.description = rs.getString("description");
                    a.icon = rs.getString("icon");
                    a.xp = rs.getInt("xp");
                    try {
                        String cat = rs.getString("category");
                        if (cat != null && !cat.isEmpty()) a.category = cat;
                    } catch (SQLException ignored) {}
                    return a;
                }
            } catch (SQLException e) {
                System.err.println("Error finding achievement by code: " + e.getMessage());
                return null;
            }
        }
    }
}
