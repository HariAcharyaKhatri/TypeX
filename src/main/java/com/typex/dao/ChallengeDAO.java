package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.Challenge;
import com.typex.model.UserChallenge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class ChallengeDAO {

    public List<UserChallenge> getCurrentChallengesForUser(int userId) {
        ensureCurrentChallenges(userId);

        List<UserChallenge> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String endOfWeekStr = endOfWeek.toString();

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT uc.*, c.type, c.title, c.description, c.requirement_type, c.target_value, " +
                    "c.xp_reward, c.start_date, c.end_date " +
                    "FROM user_challenges uc " +
                    "JOIN challenges c ON uc.challenge_id = c.id " +
                    "WHERE uc.user_id = ? AND c.end_date >= ? " +
                    "ORDER BY c.type DESC, uc.completed ASC, uc.id ASC")) {
                ps.setInt(1, userId);
                ps.setString(2, todayStr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapUserChallenge(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching current challenges: " + e.getMessage());
            }
        }
        return list;
    }

    public List<UserChallenge> recordEvent(int userId, String requirementType, double value, boolean isAccumulative) {
        List<UserChallenge> newlyCompleted = new ArrayList<>();
        List<UserChallenge> active = getCurrentChallengesForUser(userId);

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                for (UserChallenge uc : active) {
                    if (uc.completed || uc.challenge == null) continue;
                    if (!requirementType.equalsIgnoreCase(uc.challenge.requirementType)) continue;

                    double newProgress;
                    if (isAccumulative) {
                        newProgress = uc.currentProgress + value;
                    } else {
                        // For single-run thresholds (e.g. MIN_WPM, MIN_ACCURACY, PERFECT_TEST)
                        if (value >= uc.challenge.targetValue) {
                            newProgress = uc.challenge.targetValue;
                        } else {
                            newProgress = Math.max(uc.currentProgress, value);
                        }
                    }

                    boolean isDone = newProgress >= uc.challenge.targetValue;
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE user_challenges SET current_progress=?, completed=?, " +
                            "completed_at = CASE WHEN ? = 1 THEN CURRENT_TIMESTAMP ELSE completed_at END " +
                            "WHERE id=?")) {
                        ps.setDouble(1, newProgress);
                        ps.setInt(2, isDone ? 1 : 0);
                        ps.setInt(3, isDone ? 1 : 0);
                        ps.setInt(4, uc.id);
                        ps.executeUpdate();
                    }

                    if (isDone) {
                        uc.completed = true;
                        uc.currentProgress = newProgress;
                        newlyCompleted.add(uc);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error recording challenge event: " + e.getMessage());
            }
        }
        return newlyCompleted;
    }

    public int claimReward(int userId, String challengeId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                int xpReward = 0;
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT c.xp_reward FROM user_challenges uc JOIN challenges c ON uc.challenge_id = c.id " +
                        "WHERE uc.user_id=? AND uc.challenge_id=? AND uc.completed=1 AND uc.reward_claimed=0")) {
                    q.setInt(1, userId);
                    q.setString(2, challengeId);
                    try (ResultSet rs = q.executeQuery()) {
                        if (rs.next()) {
                            xpReward = rs.getInt(1);
                        }
                    }
                }
                if (xpReward > 0) {
                    try (PreparedStatement up = c.prepareStatement(
                            "UPDATE user_challenges SET reward_claimed=1 WHERE user_id=? AND challenge_id=?")) {
                        up.setInt(1, userId);
                        up.setString(2, challengeId);
                        up.executeUpdate();
                    }
                }
                return xpReward;
            } catch (SQLException e) {
                System.err.println("Error claiming challenge reward: " + e.getMessage());
                return 0;
            }
        }
    }

    private void ensureCurrentChallenges(int userId) {
        LocalDate today = LocalDate.now();
        String dayKey = today.toString();
        int dayOfYear = today.getDayOfYear();
        int year = today.getYear();

        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String weekKey = year + "_W" + ((dayOfYear / 7) + 1);

        List<Challenge> challengesToEnsure = new ArrayList<>();

        // 3 Deterministic Daily Challenges
        int d1 = (dayOfYear % 3);
        int d2 = ((dayOfYear + 1) % 3);
        int d3 = ((dayOfYear + 2) % 3);

        String idD1 = "D_" + dayKey + "_1";
        String idD2 = "D_" + dayKey + "_2";
        String idD3 = "D_" + dayKey + "_3";

        challengesToEnsure.add(createDailyTemplate(idD1, d1, dayKey, dayKey));
        challengesToEnsure.add(createDailyTemplate(idD2, d2, dayKey, dayKey));
        challengesToEnsure.add(createDailyTemplate(idD3, d3, dayKey, dayKey));

        // 2 Deterministic Weekly Challenges
        String idW1 = "W_" + weekKey + "_1";
        String idW2 = "W_" + weekKey + "_2";
        challengesToEnsure.add(new Challenge(idW1, "WEEKLY", "Weekly Dedication",
                "Practice for at least 30 minutes this week", "PRACTICE_MINUTES", 30.0, 300,
                startOfWeek.toString(), endOfWeek.toString()));
        challengesToEnsure.add(new Challenge(idW2, "WEEKLY", "Speed Builder",
                "Complete 8 typing tests this week", "TESTS_COUNT", 8.0, 250,
                startOfWeek.toString(), endOfWeek.toString()));

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                for (Challenge ch : challengesToEnsure) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT OR IGNORE INTO challenges(id, type, title, description, requirement_type, target_value, xp_reward, start_date, end_date) " +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, ch.id);
                        ps.setString(2, ch.type);
                        ps.setString(3, ch.title);
                        ps.setString(4, ch.description);
                        ps.setString(5, ch.requirementType);
                        ps.setDouble(6, ch.targetValue);
                        ps.setInt(7, ch.xpReward);
                        ps.setString(8, ch.startDate);
                        ps.setString(9, ch.endDate);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT OR IGNORE INTO user_challenges(user_id, challenge_id, current_progress, completed, reward_claimed) " +
                            "VALUES(?, ?, 0, 0, 0)")) {
                        ps.setInt(1, userId);
                        ps.setString(2, ch.id);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error ensuring current challenges: " + e.getMessage());
            }
        }
    }

    private Challenge createDailyTemplate(String id, int variant, String start, String end) {
        return switch (variant) {
            case 0 -> new Challenge(id, "DAILY", "Daily Sprint",
                    "Complete 2 typing tests today", "TESTS_COUNT", 2.0, 75, start, end);
            case 1 -> new Challenge(id, "DAILY", "Laser Focus",
                    "Achieve at least 95% accuracy in a test", "MIN_ACCURACY", 95.0, 100, start, end);
            default -> new Challenge(id, "DAILY", "Lesson Advance",
                    "Complete at least 1 lesson exercise", "LESSONS_COUNT", 1.0, 80, start, end);
        };
    }

    private UserChallenge mapUserChallenge(ResultSet rs) throws SQLException {
        UserChallenge uc = new UserChallenge();
        uc.id = rs.getInt("id");
        uc.userId = rs.getInt("user_id");
        uc.challengeId = rs.getString("challenge_id");
        uc.currentProgress = rs.getDouble("current_progress");
        uc.completed = rs.getInt("completed") == 1;
        uc.rewardClaimed = rs.getInt("reward_claimed") == 1;
        uc.completedAt = rs.getString("completed_at");

        Challenge ch = new Challenge();
        ch.id = uc.challengeId;
        ch.type = rs.getString("type");
        ch.title = rs.getString("title");
        ch.description = rs.getString("description");
        ch.requirementType = rs.getString("requirement_type");
        ch.targetValue = rs.getDouble("target_value");
        ch.xpReward = rs.getInt("xp_reward");
        ch.startDate = rs.getString("start_date");
        ch.endDate = rs.getString("end_date");
        uc.challenge = ch;

        return uc;
    }
}
