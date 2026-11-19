package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.UserXP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserXPDAO {

    public static class XPResult {
        public final int oldXp;
        public final int newXp;
        public final int oldLevel;
        public final int newLevel;
        public final boolean leveledUp;
        public final int currentLevelXp;
        public final int nextLevelXp;

        public XPResult(int oldXp, int newXp, int oldLevel, int newLevel, boolean leveledUp, int currentLevelXp, int nextLevelXp) {
            this.oldXp = oldXp;
            this.newXp = newXp;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.leveledUp = leveledUp;
            this.currentLevelXp = currentLevelXp;
            this.nextLevelXp = nextLevelXp;
        }
    }

    public static int calculateLevel(int totalXp) {
        if (totalXp <= 0) return 1;
        return 1 + (int) Math.floor(Math.sqrt(totalXp / 100.0));
    }

    public static int xpThresholdForLevel(int level) {
        if (level <= 1) return 0;
        int prev = level - 1;
        return 100 * prev * prev;
    }

    public static int xpThresholdForNextLevel(int level) {
        return 100 * level * level;
    }

    public UserXP getForUser(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureUserXpExists(c, userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT total_xp, level FROM user_xp WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int total = rs.getInt("total_xp");
                            int lvl = rs.getInt("level");
                            int curThresh = xpThresholdForLevel(lvl);
                            int nextThresh = xpThresholdForNextLevel(lvl);
                            return new UserXP(userId, total, lvl, curThresh, nextThresh);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching user XP: " + e.getMessage());
            }
        }
        return new UserXP(userId, 0, 1, 0, 100);
    }

    public XPResult addXP(int userId, int amount) {
        if (amount <= 0) {
            UserXP cur = getForUser(userId);
            return new XPResult(cur.totalXp, cur.totalXp, cur.level, cur.level, false, cur.currentLevelXp, cur.nextLevelXp);
        }

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureUserXpExists(c, userId);
                int oldXp = 0;
                int oldLevel = 1;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT total_xp, level FROM user_xp WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldXp = rs.getInt("total_xp");
                            oldLevel = rs.getInt("level");
                        }
                    }
                }

                int newXp = oldXp + amount;
                int newLevel = calculateLevel(newXp);
                boolean leveledUp = newLevel > oldLevel;

                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE user_xp SET total_xp=?, level=? WHERE user_id=?")) {
                    up.setInt(1, newXp);
                    up.setInt(2, newLevel);
                    up.setInt(3, userId);
                    up.executeUpdate();
                }

                int curThresh = xpThresholdForLevel(newLevel);
                int nextThresh = xpThresholdForNextLevel(newLevel);
                return new XPResult(oldXp, newXp, oldLevel, newLevel, leveledUp, curThresh, nextThresh);
            } catch (SQLException e) {
                System.err.println("Error adding user XP: " + e.getMessage());
                return new XPResult(0, 0, 1, 1, false, 0, 100);
            }
        }
    }

    private void ensureUserXpExists(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO user_xp(user_id, total_xp, level) VALUES(?, 0, 1)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
