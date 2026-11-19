package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.PracticeStreak;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PracticeStreakDAO {

    public static class StreakUpdateResult {
        public final int currentStreak;
        public final int longestStreak;
        public final boolean continued;
        public final boolean broken;
        public final boolean alreadyCountedToday;

        public StreakUpdateResult(int currentStreak, int longestStreak, boolean continued, boolean broken, boolean alreadyCountedToday) {
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.continued = continued;
            this.broken = broken;
            this.alreadyCountedToday = alreadyCountedToday;
        }
    }

    public PracticeStreak getForUser(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureStreakRowExists(c, userId);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT current_streak, longest_streak, last_practice_date FROM practice_streaks WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int current = rs.getInt("current_streak");
                            int longest = rs.getInt("longest_streak");
                            String lastDateStr = rs.getString("last_practice_date");

                            if (lastDateStr != null && !lastDateStr.isEmpty()) {
                                LocalDate last = LocalDate.parse(lastDateStr);
                                LocalDate today = LocalDate.now();
                                LocalDate yesterday = today.minusDays(1);
                                if (!last.equals(today) && !last.equals(yesterday)) {
                                    // Streak is broken
                                    current = 0;
                                }
                            }
                            return new PracticeStreak(userId, current, longest, lastDateStr);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching practice streak: " + e.getMessage());
            }
        }
        return new PracticeStreak(userId, 0, 0, null);
    }

    public StreakUpdateResult recordQualifyingActivity(int userId) {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureStreakRowExists(c, userId);

                int current = 0;
                int longest = 0;
                String lastDateStr = null;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT current_streak, longest_streak, last_practice_date FROM practice_streaks WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            current = rs.getInt("current_streak");
                            longest = rs.getInt("longest_streak");
                            lastDateStr = rs.getString("last_practice_date");
                        }
                    }
                }

                if (todayStr.equals(lastDateStr)) {
                    // Already counted today
                    return new StreakUpdateResult(current, longest, false, false, true);
                }

                boolean continued = false;
                boolean broken = false;

                if (lastDateStr == null || lastDateStr.isEmpty()) {
                    current = 1;
                    continued = true;
                } else {
                    LocalDate lastDate = LocalDate.parse(lastDateStr);
                    LocalDate yesterday = today.minusDays(1);
                    if (lastDate.equals(yesterday)) {
                        current += 1;
                        continued = true;
                    } else {
                        current = 1;
                        broken = true;
                    }
                }

                longest = Math.max(longest, current);

                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE practice_streaks SET current_streak=?, longest_streak=?, last_practice_date=? WHERE user_id=?")) {
                    up.setInt(1, current);
                    up.setInt(2, longest);
                    up.setString(3, todayStr);
                    up.setInt(4, userId);
                    up.executeUpdate();
                }

                return new StreakUpdateResult(current, longest, continued, broken, false);
            } catch (SQLException e) {
                System.err.println("Error recording practice streak: " + e.getMessage());
                return new StreakUpdateResult(0, 0, false, false, false);
            }
        }
    }

    private void ensureStreakRowExists(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO practice_streaks(user_id, current_streak, longest_streak, last_practice_date) VALUES(?, 0, 0, NULL)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
