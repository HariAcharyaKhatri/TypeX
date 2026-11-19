package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.CharacterError;

import java.sql.*;
import java.util.*;

public class CharacterErrorDAO {
    public void recordErrors(int userId, Map<Character, Integer> errors) {
        if (errors == null || errors.isEmpty()) return;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try {
                DatabaseManager.getInstance().executeTransaction(conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO character_errors(user_id, char_code, char_display, error_count, total_attempts, last_mistyped) " +
                            "VALUES(?,?,?,?,?,CURRENT_TIMESTAMP) " +
                            "ON CONFLICT(user_id, char_code) DO UPDATE SET " +
                            "error_count = error_count + excluded.error_count, " +
                            "last_mistyped = CURRENT_TIMESTAMP")) {
                        for (Map.Entry<Character, Integer> entry : errors.entrySet()) {
                            char ch = entry.getKey();
                            int count = entry.getValue();
                            if (count <= 0) continue;
                            ps.setInt(1, userId);
                            ps.setInt(2, (int) ch);
                            ps.setString(3, ch == ' ' ? "Space" : String.valueOf(ch));
                            ps.setInt(4, count);
                            ps.setInt(5, count);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                });
            } catch (SQLException e) {
                System.err.println("Error recording character errors: " + e.getMessage());
            }
        }
    }

    public List<CharacterError> getTopErrors(int userId, int limit) {
        List<CharacterError> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM character_errors WHERE user_id=? ORDER BY error_count DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CharacterError ce = new CharacterError();
                        ce.id = rs.getInt("id");
                        ce.userId = rs.getInt("user_id");
                        ce.charCode = rs.getInt("char_code");
                        ce.charDisplay = rs.getString("char_display");
                        ce.errorCount = rs.getInt("error_count");
                        ce.totalAttempts = rs.getInt("total_attempts");
                        ce.lastMistyped = rs.getString("last_mistyped");
                        out.add(ce);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching top character errors: " + e.getMessage());
            }
        }
        return out;
    }

    public Map<Character, Integer> getErrorCountMap(int userId) {
        Map<Character, Integer> map = new HashMap<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT char_code, error_count FROM character_errors WHERE user_id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.put((char) rs.getInt("char_code"), rs.getInt("error_count"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching character error map: " + e.getMessage());
            }
        }
        return map;
    }
}
