package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.PersonalBest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonalBestDAO {
    /**
     * Records a result. If it's a new personal best for (userId, category, duration), updates or inserts and returns true.
     */
    public boolean recordCandidate(int userId, String category, int durationSeconds, double wpm, double accuracy) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                PersonalBest current = getBest(userId, category, durationSeconds);
                if (current == null) {
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO personal_bests(user_id, category, duration_seconds, best_wpm, accuracy, achieved_at) " +
                            "VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)")) {
                        ins.setInt(1, userId);
                        ins.setString(2, category);
                        ins.setInt(3, durationSeconds);
                        ins.setDouble(4, wpm);
                        ins.setDouble(5, accuracy);
                        ins.executeUpdate();
                        return true;
                    }
                } else if (wpm > current.bestWpm) {
                    try (PreparedStatement upd = c.prepareStatement(
                            "UPDATE personal_bests SET best_wpm=?, accuracy=?, achieved_at=CURRENT_TIMESTAMP WHERE id=?")) {
                        upd.setDouble(1, wpm);
                        upd.setDouble(2, accuracy);
                        upd.setInt(3, current.id);
                        upd.executeUpdate();
                        return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error recording personal best: " + e.getMessage());
            }
        }
        return false;
    }

    public PersonalBest getBest(int userId, String category, int durationSeconds) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM personal_bests WHERE user_id=? AND category=? AND duration_seconds=?")) {
                ps.setInt(1, userId);
                ps.setString(2, category);
                ps.setInt(3, durationSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PersonalBest pb = new PersonalBest();
                        pb.id = rs.getInt("id");
                        pb.userId = rs.getInt("user_id");
                        pb.category = rs.getString("category");
                        pb.durationSeconds = rs.getInt("duration_seconds");
                        pb.bestWpm = rs.getDouble("best_wpm");
                        pb.accuracy = rs.getDouble("accuracy");
                        pb.achievedAt = rs.getString("achieved_at");
                        return pb;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching personal best: " + e.getMessage());
            }
        }
        return null;
    }

    public List<PersonalBest> getAllBest(int userId) {
        List<PersonalBest> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM personal_bests WHERE user_id=? ORDER BY best_wpm DESC")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PersonalBest pb = new PersonalBest();
                        pb.id = rs.getInt("id");
                        pb.userId = rs.getInt("user_id");
                        pb.category = rs.getString("category");
                        pb.durationSeconds = rs.getInt("duration_seconds");
                        pb.bestWpm = rs.getDouble("best_wpm");
                        pb.accuracy = rs.getDouble("accuracy");
                        pb.achievedAt = rs.getString("achieved_at");
                        out.add(pb);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching personal bests: " + e.getMessage());
            }
        }
        return out;
    }
}
