package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.KeyboardSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KeyboardSessionDAO {
    public int insert(KeyboardSession s) {
        if (s == null) return -1;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO keyboard_sessions(user_id, drill_type, keys_tested, correct_keys, accuracy, avg_reaction_time_ms) " +
                    "VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, s.userId);
                ps.setString(2, s.drillType);
                ps.setInt(3, s.keysTested);
                ps.setInt(4, s.correctKeys);
                ps.setDouble(5, s.accuracy);
                ps.setDouble(6, s.avgReactionTimeMs);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        s.id = rs.getInt(1);
                        return s.id;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error inserting keyboard session: " + e.getMessage());
            }
        }
        return -1;
    }

    public List<KeyboardSession> findByUser(int userId, int limit) {
        List<KeyboardSession> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM keyboard_sessions WHERE user_id=? ORDER BY completed_at DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        KeyboardSession s = new KeyboardSession();
                        s.id = rs.getInt("id");
                        s.userId = rs.getInt("user_id");
                        s.drillType = rs.getString("drill_type");
                        s.keysTested = rs.getInt("keys_tested");
                        s.correctKeys = rs.getInt("correct_keys");
                        s.accuracy = rs.getDouble("accuracy");
                        s.avgReactionTimeMs = rs.getDouble("avg_reaction_time_ms");
                        s.completedAt = rs.getString("completed_at");
                        out.add(s);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching keyboard sessions: " + e.getMessage());
            }
        }
        return out;
    }
}
