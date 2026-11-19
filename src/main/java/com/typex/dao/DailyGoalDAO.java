package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.DailyGoal;
import com.typex.model.UserPreference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class DailyGoalDAO {

    public DailyGoal getOrCreateToday(int userId, UserPreference pref) {
        String today = LocalDate.now().toString();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureTodayExists(c, userId, today, pref);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM daily_goals WHERE user_id=? AND date=?")) {
                    ps.setInt(1, userId);
                    ps.setString(2, today);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return map(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching today's daily goal: " + e.getMessage());
            }
        }
        DailyGoal fallback = new DailyGoal();
        fallback.userId = userId;
        fallback.date = today;
        return fallback;
    }

    public boolean recordTest(int userId, int durationSec, double accuracy) {
        String today = LocalDate.now().toString();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureTodayExists(c, userId, today, null);
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE daily_goals SET completed_seconds = completed_seconds + ?, " +
                        "completed_tests = completed_tests + 1 WHERE user_id=? AND date=?")) {
                    up.setInt(1, Math.max(0, durationSec));
                    up.setInt(2, userId);
                    up.setString(3, today);
                    up.executeUpdate();
                }
                return evaluateAchieved(c, userId, today);
            } catch (SQLException e) {
                System.err.println("Error updating daily goal test: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean recordLesson(int userId, int durationSec, boolean passed) {
        String today = LocalDate.now().toString();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                ensureTodayExists(c, userId, today, null);
                int lessonInc = passed ? 1 : 0;
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE daily_goals SET completed_seconds = completed_seconds + ?, " +
                        "completed_lessons = completed_lessons + ? WHERE user_id=? AND date=?")) {
                    up.setInt(1, Math.max(0, durationSec));
                    up.setInt(2, lessonInc);
                    up.setInt(3, userId);
                    up.setString(4, today);
                    up.executeUpdate();
                }
                return evaluateAchieved(c, userId, today);
            } catch (SQLException e) {
                System.err.println("Error updating daily goal lesson: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean claimReward(int userId, String date) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE daily_goals SET reward_claimed=1 WHERE user_id=? AND date=? AND achieved=1 AND reward_claimed=0")) {
                    ps.setInt(1, userId);
                    ps.setString(2, date);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                System.err.println("Error claiming daily goal reward: " + e.getMessage());
                return false;
            }
        }
    }

    private boolean evaluateAchieved(Connection c, int userId, String today) throws SQLException {
        DailyGoal goal = null;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM daily_goals WHERE user_id=? AND date=?")) {
            ps.setInt(1, userId);
            ps.setString(2, today);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) goal = map(rs);
            }
        }
        if (goal == null) return false;

        boolean nowAchieved = (goal.completedSeconds >= goal.targetMinutes * 60)
                && (goal.completedTests >= goal.targetTests)
                && (goal.completedLessons >= goal.targetLessons);

        if (nowAchieved && !goal.achieved) {
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE daily_goals SET achieved=1 WHERE id=?")) {
                up.setInt(1, goal.id);
                up.executeUpdate();
            }
            return true; // Newly achieved!
        }
        return false;
    }

    private void ensureTodayExists(Connection c, int userId, String today, UserPreference pref) throws SQLException {
        int targetMin = pref != null ? pref.dailyGoalMinutes : 15;
        int targetTst = pref != null ? pref.dailyGoalTests : 3;
        int targetLes = pref != null ? pref.dailyGoalLessons : 1;
        double targetAcc = pref != null ? pref.dailyGoalAccuracy : 95.0;

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO daily_goals(user_id, date, target_minutes, target_tests, target_lessons, target_accuracy, " +
                "completed_seconds, completed_tests, completed_lessons, achieved, reward_claimed) " +
                "VALUES(?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0)")) {
            ps.setInt(1, userId);
            ps.setString(2, today);
            ps.setInt(3, targetMin);
            ps.setInt(4, targetTst);
            ps.setInt(5, targetLes);
            ps.setDouble(6, targetAcc);
            ps.executeUpdate();
        }
    }

    private DailyGoal map(ResultSet rs) throws SQLException {
        DailyGoal g = new DailyGoal();
        g.id = rs.getInt("id");
        g.userId = rs.getInt("user_id");
        g.date = rs.getString("date");
        g.targetMinutes = rs.getInt("target_minutes");
        g.targetTests = rs.getInt("target_tests");
        g.targetLessons = rs.getInt("target_lessons");
        g.targetAccuracy = rs.getDouble("target_accuracy");
        g.completedSeconds = rs.getInt("completed_seconds");
        g.completedTests = rs.getInt("completed_tests");
        g.completedLessons = rs.getInt("completed_lessons");
        g.achieved = rs.getInt("achieved") == 1;
        g.rewardClaimed = rs.getInt("reward_claimed") == 1;
        return g;
    }
}
