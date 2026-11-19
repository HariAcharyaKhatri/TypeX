package com.typex.service;

import com.typex.dao.LessonAttemptDAO;
import com.typex.database.DatabaseManager;
import com.typex.model.Lesson;
import com.typex.model.LessonAttempt;

import java.sql.*;
import java.util.Collections;
import java.util.List;

public class LessonService {
    private final LessonAttemptDAO attemptDAO = new LessonAttemptDAO();

    public boolean isCompleted(int userId, int lessonId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT completed FROM lesson_progress WHERE user_id=? AND lesson_id=?")) {
                ps.setInt(1, userId);
                ps.setInt(2, lessonId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) == 1;
                }
            } catch (SQLException e) {
                System.err.println("Error checking lesson completion: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean isLessonUnlocked(int userId, Lesson lesson, List<Lesson> allLessons) {
        if (lesson == null || allLessons == null || allLessons.isEmpty()) return true;
        // The very first lesson is always unlocked
        if (lesson.level == 1 && lesson.orderIndex == 1) return true;

        // Find index of this lesson
        int idx = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).id == lesson.id) {
                idx = i;
                break;
            }
        }
        if (idx <= 0) return true;

        // Previous lesson must be completed
        Lesson prev = allLessons.get(idx - 1);
        return isCompleted(userId, prev.id);
    }

    public void recordAttempt(int userId, Lesson lesson, double wpm, double accuracy, int durationSeconds) {
        if (lesson == null) return;
        boolean meetsRequirements = (accuracy >= lesson.minAccuracy) && (wpm >= lesson.minWpm);

        // Record individual attempt in lesson_attempts (Module 6)
        LessonAttempt attempt = new LessonAttempt();
        attempt.userId = userId;
        attempt.lessonId = lesson.id;
        attempt.wpm = wpm;
        attempt.accuracy = accuracy;
        attempt.durationSeconds = durationSeconds;
        attempt.completed = meetsRequirements;
        attemptDAO.insert(attempt);

        // Update overall lesson progress
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id, completed, best_wpm, best_accuracy, attempts FROM lesson_progress " +
                        "WHERE user_id=? AND lesson_id=?")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, lesson.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt("id");
                            int wasCompleted = rs.getInt("completed");
                            double bestWpm = Math.max(rs.getDouble("best_wpm"), wpm);
                            double bestAcc = Math.max(rs.getDouble("best_accuracy"), accuracy);
                            try (PreparedStatement u = c.prepareStatement(
                                    "UPDATE lesson_progress SET completed=?, best_wpm=?, best_accuracy=?, " +
                                    "attempts=attempts+1, last_attempt=CURRENT_TIMESTAMP WHERE id=?")) {
                                u.setInt(1, (meetsRequirements || wasCompleted == 1) ? 1 : 0);
                                u.setDouble(2, bestWpm);
                                u.setDouble(3, bestAcc);
                                u.setInt(4, id);
                                u.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement i = c.prepareStatement(
                                    "INSERT INTO lesson_progress(user_id,lesson_id,completed,best_wpm,best_accuracy,attempts,last_attempt) " +
                                    "VALUES(?,?,?,?,?,1,CURRENT_TIMESTAMP)")) {
                                i.setInt(1, userId);
                                i.setInt(2, lesson.id);
                                i.setInt(3, meetsRequirements ? 1 : 0);
                                i.setDouble(4, wpm);
                                i.setDouble(5, accuracy);
                                i.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error recording lesson attempt: " + e.getMessage());
            }
        }
    }

    public List<LessonAttempt> getAttemptHistory(int userId, int lessonId) {
        return attemptDAO.findByLessonAndUser(userId, lessonId, 8);
    }

    public int completedCount(int userId) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM lesson_progress WHERE user_id=? AND completed=1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                System.err.println("Error counting completed lessons: " + e.getMessage());
                return 0;
            }
        }
    }

    public double completionPercent(int userId, List<Lesson> all) {
        if (all == null || all.isEmpty()) return 0.0;
        return (completedCount(userId) * 100.0) / all.size();
    }
}
