package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.DailyStat;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class StatisticsDAO {
    public void recordTest(int userId, double wpm, double accuracy, int durationSec, int wordsTyped) {
        String today = LocalDate.now().toString();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id, practice_seconds, words_typed, tests_completed, avg_wpm, avg_accuracy " +
                        "FROM daily_statistics WHERE user_id=? AND date=?")) {
                    ps.setInt(1, userId);
                    ps.setString(2, today);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt("id");
                            int prevSecs = rs.getInt("practice_seconds");
                            int prevWords = rs.getInt("words_typed");
                            int prevTests = rs.getInt("tests_completed");
                            double prevWpm = rs.getDouble("avg_wpm");
                            double prevAcc = rs.getDouble("avg_accuracy");

                            int newTests = prevTests + 1;
                            double newWpm = ((prevWpm * prevTests) + wpm) / newTests;
                            double newAcc = ((prevAcc * prevTests) + accuracy) / newTests;

                            try (PreparedStatement u = c.prepareStatement(
                                    "UPDATE daily_statistics SET practice_seconds=?, words_typed=?, " +
                                    "tests_completed=?, avg_wpm=?, avg_accuracy=? WHERE id=?")) {
                                u.setInt(1, prevSecs + durationSec);
                                u.setInt(2, prevWords + wordsTyped);
                                u.setInt(3, newTests);
                                u.setDouble(4, newWpm);
                                u.setDouble(5, newAcc);
                                u.setInt(6, id);
                                u.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement i = c.prepareStatement(
                                    "INSERT INTO daily_statistics(user_id,date,practice_seconds,words_typed," +
                                    "tests_completed,avg_wpm,avg_accuracy) VALUES(?,?,?,?,?,?,?)")) {
                                i.setInt(1, userId);
                                i.setString(2, today);
                                i.setInt(3, durationSec);
                                i.setInt(4, wordsTyped);
                                i.setInt(5, 1);
                                i.setDouble(6, wpm);
                                i.setDouble(7, accuracy);
                                i.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error recording daily test stat: " + e.getMessage());
            }
        }
    }

    public List<DailyStat> recent(int userId, int days) {
        List<DailyStat> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM daily_statistics WHERE user_id=? ORDER BY date DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, days));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching recent stats: " + e.getMessage());
            }
        }
        Collections.reverse(out);
        return out;
    }

    public long totalPracticeSeconds(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(practice_seconds), 0) FROM daily_statistics WHERE user_id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            } catch (SQLException e) {
                System.err.println("Error fetching total practice seconds: " + e.getMessage());
                return 0L;
            }
        }
    }

    public int currentStreak(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT date FROM daily_statistics WHERE user_id=? AND (tests_completed > 0 OR lessons_completed > 0) ORDER BY date DESC")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return 0;

                    LocalDate today = LocalDate.now();
                    LocalDate yesterday = today.minusDays(1);
                    LocalDate firstDate = LocalDate.parse(rs.getString(1));

                    // If latest practice was neither today nor yesterday, streak is broken
                    if (!firstDate.equals(today) && !firstDate.equals(yesterday)) {
                        return 0;
                    }

                    int streak = 1;
                    LocalDate expected = firstDate.minusDays(1);

                    while (rs.next()) {
                        LocalDate d = LocalDate.parse(rs.getString(1));
                        if (d.equals(expected)) {
                            streak++;
                            expected = expected.minusDays(1);
                        } else if (d.isBefore(expected)) {
                            break;
                        }
                    }
                    return streak;
                }
            } catch (SQLException e) {
                System.err.println("Error calculating streak: " + e.getMessage());
                return 0;
            }
        }
    }

    private DailyStat map(ResultSet rs) throws SQLException {
        DailyStat d = new DailyStat();
        d.date = rs.getString("date");
        d.practiceSeconds = rs.getInt("practice_seconds");
        d.wordsTyped = rs.getInt("words_typed");
        d.testsCompleted = rs.getInt("tests_completed");
        d.lessonsCompleted = rs.getInt("lessons_completed");
        d.avgWpm = rs.getDouble("avg_wpm");
        d.avgAccuracy = rs.getDouble("avg_accuracy");
        return d;
    }
}
