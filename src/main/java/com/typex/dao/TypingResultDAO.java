package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.TypingResult;

import java.sql.*;
import java.util.*;

public class TypingResultDAO {
    public int insert(TypingResult r) {
        if (r == null) return -1;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO typing_test_results(user_id,category,duration_seconds,gross_wpm,net_wpm,accuracy," +
                    "correct_chars,incorrect_chars,total_chars,total_keystrokes,backspaces,error_percent) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.userId);
                ps.setString(2, r.category);
                ps.setInt(3, r.durationSeconds);
                ps.setDouble(4, r.grossWpm);
                ps.setDouble(5, r.netWpm);
                ps.setDouble(6, r.accuracy);
                ps.setInt(7, r.correctChars);
                ps.setInt(8, r.incorrectChars);
                ps.setInt(9, r.totalChars);
                ps.setInt(10, r.totalKeystrokes);
                ps.setInt(11, r.backspaces);
                ps.setDouble(12, r.errorPercent);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.id = rs.getInt(1);
                        return r.id;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error inserting typing result: " + e.getMessage());
            }
        }
        return -1;
    }

    public List<TypingResult> findByUser(int userId, int limit) {
        List<TypingResult> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM typing_test_results WHERE user_id=? ORDER BY completed_at DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching results by user: " + e.getMessage());
            }
        }
        return out;
    }

    public TypingResult findBestByUser(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM typing_test_results WHERE user_id=? ORDER BY net_wpm DESC LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            } catch (SQLException e) {
                System.err.println("Error fetching best result: " + e.getMessage());
                return null;
            }
        }
    }

    public int countByUser(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM typing_test_results WHERE user_id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                System.err.println("Error counting user results: " + e.getMessage());
                return 0;
            }
        }
    }

    private TypingResult map(ResultSet rs) throws SQLException {
        TypingResult r = new TypingResult();
        r.id = rs.getInt("id");
        r.userId = rs.getInt("user_id");
        r.category = rs.getString("category");
        r.durationSeconds = rs.getInt("duration_seconds");
        r.grossWpm = rs.getDouble("gross_wpm");
        r.netWpm = rs.getDouble("net_wpm");
        r.accuracy = rs.getDouble("accuracy");
        r.correctChars = rs.getInt("correct_chars");
        r.incorrectChars = rs.getInt("incorrect_chars");
        r.totalChars = rs.getInt("total_chars");
        r.totalKeystrokes = rs.getInt("total_keystrokes");
        r.backspaces = rs.getInt("backspaces");
        r.errorPercent = rs.getDouble("error_percent");
        r.completedAt = rs.getString("completed_at");
        return r;
    }
}
