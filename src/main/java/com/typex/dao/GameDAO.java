package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.GamePersonalBest;
import com.typex.model.GameSession;

import java.sql.*;
import java.util.*;

public class GameDAO {

    public boolean recordSession(GameSession session) {
        if (session == null || session.userId <= 0) return false;
        final boolean[] isNewPb = new boolean[]{false};

        try {
            DatabaseManager.getInstance().executeTransaction(c -> {
                // 1. Insert session record
                String insertSql = """
                    INSERT INTO game_sessions(user_id, game_type, score, level_reached, words_completed,
                                              accuracy, wpm, duration_seconds, xp_earned)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
                try (PreparedStatement ps = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, session.userId);
                    ps.setString(2, session.gameType);
                    ps.setInt(3, session.score);
                    ps.setInt(4, Math.max(1, session.levelReached));
                    ps.setInt(5, session.wordsCompleted);
                    ps.setDouble(6, session.accuracy);
                    ps.setDouble(7, session.wpm);
                    ps.setInt(8, session.durationSeconds);
                    ps.setInt(9, session.xpEarned);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            session.id = rs.getInt(1);
                        }
                    }
                }

                // 2. Check and update personal best atomically
                GamePersonalBest existingPb = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM game_personal_bests WHERE user_id = ? AND game_type = ?")) {
                    ps.setInt(1, session.userId);
                    ps.setString(2, session.gameType);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingPb = new GamePersonalBest();
                            existingPb.id = rs.getInt("id");
                            existingPb.userId = rs.getInt("user_id");
                            existingPb.gameType = rs.getString("game_type");
                            existingPb.highScore = rs.getInt("high_score");
                            existingPb.highestLevel = rs.getInt("highest_level");
                            existingPb.bestWpm = rs.getDouble("best_wpm");
                            existingPb.bestAccuracy = rs.getDouble("best_accuracy");
                            existingPb.achievedAt = rs.getString("achieved_at");
                        }
                    }
                }

                if (existingPb == null) {
                    if (session.score > 0 || session.levelReached > 1 || session.wpm > 0 || session.accuracy > 0) {
                        isNewPb[0] = true;
                        String insPb = """
                            INSERT INTO game_personal_bests(user_id, game_type, high_score, highest_level,
                                                            best_wpm, best_accuracy)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """;
                        try (PreparedStatement ps = c.prepareStatement(insPb)) {
                            ps.setInt(1, session.userId);
                            ps.setString(2, session.gameType);
                            ps.setInt(3, session.score);
                            ps.setInt(4, Math.max(1, session.levelReached));
                            ps.setDouble(5, session.wpm);
                            ps.setDouble(6, session.accuracy);
                            ps.executeUpdate();
                        }
                    }
                } else {
                    boolean scoreImproved = session.score > existingPb.highScore;
                    boolean levelImproved = session.levelReached > existingPb.highestLevel;
                    boolean wpmImproved = session.wpm > existingPb.bestWpm;
                    boolean accImproved = session.accuracy > existingPb.bestAccuracy;

                    if (scoreImproved || levelImproved || wpmImproved || accImproved) {
                        isNewPb[0] = true;
                        String updPb = """
                            UPDATE game_personal_bests
                            SET high_score = MAX(high_score, ?),
                                highest_level = MAX(highest_level, ?),
                                best_wpm = MAX(best_wpm, ?),
                                best_accuracy = MAX(best_accuracy, ?),
                                achieved_at = CURRENT_TIMESTAMP
                            WHERE user_id = ? AND game_type = ?
                        """;
                        try (PreparedStatement ps = c.prepareStatement(updPb)) {
                            ps.setInt(1, session.score);
                            ps.setInt(2, session.levelReached);
                            ps.setDouble(3, session.wpm);
                            ps.setDouble(4, session.accuracy);
                            ps.setInt(5, session.userId);
                            ps.setString(6, session.gameType);
                            ps.executeUpdate();
                        }
                    }
                }
            });
        } catch (SQLException e) {
            System.err.println("Error recording game session atomically: " + e.getMessage());
            throw new com.typex.database.DataAccessException("Failed to record game session: " + e.getMessage(), e);
        }

        return isNewPb[0];
    }

    public boolean isPersonalBest(int userId, String gameType, int score, int level, double wpm, double acc) {
        if (userId <= 0 || gameType == null) return false;
        GamePersonalBest pb = getPersonalBest(userId, gameType);
        if (pb == null) {
            return score > 0 || level > 1 || wpm > 0 || acc > 0;
        }
        return score > pb.highScore || level > pb.highestLevel || wpm > pb.bestWpm || acc > pb.bestAccuracy;
    }

    public boolean isPersonalBest(int userId, String gameType, int score, double wpm, double acc) {
        return isPersonalBest(userId, gameType, score, 1, wpm, acc);
    }

    public void updateSessionXp(int sessionId, int xpEarned) {
        if (sessionId <= 0) return;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE game_sessions SET xp_earned = ? WHERE id = ?")) {
                ps.setInt(1, xpEarned);
                ps.setInt(2, sessionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error updating session XP: " + e.getMessage());
            }
        }
    }

    public GamePersonalBest getPersonalBest(int userId, String gameType) {
        if (userId <= 0 || gameType == null) return null;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM game_personal_bests WHERE user_id = ? AND game_type = ?")) {
                ps.setInt(1, userId);
                ps.setString(2, gameType);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        GamePersonalBest pb = new GamePersonalBest();
                        pb.id = rs.getInt("id");
                        pb.userId = rs.getInt("user_id");
                        pb.gameType = rs.getString("game_type");
                        pb.highScore = rs.getInt("high_score");
                        pb.highestLevel = rs.getInt("highest_level");
                        pb.bestWpm = rs.getDouble("best_wpm");
                        pb.bestAccuracy = rs.getDouble("best_accuracy");
                        pb.achievedAt = rs.getString("achieved_at");
                        return pb;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching game personal best: " + e.getMessage());
            }
        }
        return null;
    }

    public Map<String, GamePersonalBest> getAllPersonalBests(int userId) {
        Map<String, GamePersonalBest> map = new HashMap<>();
        if (userId <= 0) return map;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM game_personal_bests WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        GamePersonalBest pb = new GamePersonalBest();
                        pb.id = rs.getInt("id");
                        pb.userId = rs.getInt("user_id");
                        pb.gameType = rs.getString("game_type");
                        pb.highScore = rs.getInt("high_score");
                        pb.highestLevel = rs.getInt("highest_level");
                        pb.bestWpm = rs.getDouble("best_wpm");
                        pb.bestAccuracy = rs.getDouble("best_accuracy");
                        pb.achievedAt = rs.getString("achieved_at");
                        map.put(pb.gameType.toLowerCase(), pb);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching all game personal bests: " + e.getMessage());
            }
        }
        return map;
    }

    public List<GameSession> getRecentSessions(int userId, int limit) {
        List<GameSession> list = new ArrayList<>();
        if (userId <= 0) return list;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM game_sessions WHERE user_id = ? ORDER BY played_at DESC LIMIT ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        GameSession s = new GameSession();
                        s.id = rs.getInt("id");
                        s.userId = rs.getInt("user_id");
                        s.gameType = rs.getString("game_type");
                        s.score = rs.getInt("score");
                        s.levelReached = rs.getInt("level_reached");
                        s.wordsCompleted = rs.getInt("words_completed");
                        s.accuracy = rs.getDouble("accuracy");
                        s.wpm = rs.getDouble("wpm");
                        s.durationSeconds = rs.getInt("duration_seconds");
                        s.xpEarned = rs.getInt("xp_earned");
                        s.playedAt = rs.getString("played_at");
                        list.add(s);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching recent game sessions: " + e.getMessage());
            }
        }
        return list;
    }

    public int getTotalGamesPlayed(int userId) {
        if (userId <= 0) return 0;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM game_sessions WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                System.err.println("Error counting games played: " + e.getMessage());
            }
        }
        return 0;
    }

    public int getTotalScore(int userId) {
        if (userId <= 0) return 0;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(high_score), 0) FROM game_personal_bests WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                System.err.println("Error summing game scores: " + e.getMessage());
            }
        }
        return 0;
    }

    public String getFavoriteGameType(int userId) {
        if (userId <= 0) return null;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT game_type, COUNT(*) as cnt FROM game_sessions WHERE user_id = ? " +
                     "GROUP BY game_type ORDER BY cnt DESC, game_type ASC LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("game_type");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error calculating favorite game type: " + e.getMessage());
            }
        }
        return null;
    }
}
