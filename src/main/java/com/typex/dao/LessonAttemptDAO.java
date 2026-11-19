package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.LessonAttempt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LessonAttemptDAO {
    public int insert(LessonAttempt a) {
        if (a == null) return -1;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lesson_attempts(user_id, lesson_id, wpm, accuracy, duration_seconds, completed) " +
                    "VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, a.userId);
                ps.setInt(2, a.lessonId);
                ps.setDouble(3, a.wpm);
                ps.setDouble(4, a.accuracy);
                ps.setInt(5, a.durationSeconds);
                ps.setInt(6, a.completed ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        a.id = rs.getInt(1);
                        return a.id;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error inserting lesson attempt: " + e.getMessage());
            }
        }
        return -1;
    }

    public List<LessonAttempt> findByLessonAndUser(int userId, int lessonId, int limit) {
        List<LessonAttempt> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM lesson_attempts WHERE user_id=? AND lesson_id=? ORDER BY attempted_at DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, lessonId);
                ps.setInt(3, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LessonAttempt a = new LessonAttempt();
                        a.id = rs.getInt("id");
                        a.userId = rs.getInt("user_id");
                        a.lessonId = rs.getInt("lesson_id");
                        a.wpm = rs.getDouble("wpm");
                        a.accuracy = rs.getDouble("accuracy");
                        a.durationSeconds = rs.getInt("duration_seconds");
                        a.completed = rs.getInt("completed") == 1;
                        a.attemptedAt = rs.getString("attempted_at");
                        out.add(a);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching lesson attempts: " + e.getMessage());
            }
        }
        return out;
    }
}
