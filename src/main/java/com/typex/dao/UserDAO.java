package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.User;

import java.sql.*;
import java.util.*;

public class UserDAO {
    public List<User> findAll() {
        List<User> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY username")) {
                while (rs.next()) out.add(map(rs));
            } catch (SQLException e) {
                System.err.println("Error fetching users: " + e.getMessage());
            }
        }
        return out;
    }

    public User findById(int id) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            } catch (SQLException e) {
                System.err.println("Error finding user by id: " + e.getMessage());
                return null;
            }
        }
    }

    public User findByUsername(String username) {
        if (username == null) return null;
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            } catch (SQLException e) {
                System.err.println("Error finding user by username: " + e.getMessage());
                return null;
            }
        }
    }

    public User create(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        String cleanName = username.trim();
        final int[] newId = new int[1];

        try {
            DatabaseManager.getInstance().executeTransaction(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cleanName);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            newId[0] = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to obtain generated key for new user");
                        }
                    }
                }
                try (PreparedStatement s2 = conn.prepareStatement(
                        "INSERT INTO user_settings(user_id) VALUES(?)")) {
                    s2.setInt(1, newId[0]);
                    s2.executeUpdate();
                }
            });
            return findById(newId[0]);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    public void update(User u) {
        if (u == null || u.username == null || u.username.trim().isEmpty()) {
            throw new IllegalArgumentException("Valid user with non-empty username required");
        }
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE users SET username=?, avatar_path=?, language=?, skill_level=?, daily_goal=? WHERE id=?")) {
                ps.setString(1, u.username.trim());
                ps.setString(2, u.avatarPath);
                ps.setString(3, u.language != null ? u.language : "en");
                ps.setString(4, u.skillLevel != null ? u.skillLevel : "Beginner");
                ps.setInt(5, u.dailyGoal > 0 ? u.dailyGoal : 30);
                ps.setInt(6, u.id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
            }
        }
    }

    public void delete(int id) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.id = rs.getInt("id");
        u.username = rs.getString("username");
        u.avatarPath = rs.getString("avatar_path");
        u.language = rs.getString("language");
        u.skillLevel = rs.getString("skill_level");
        u.dailyGoal = rs.getInt("daily_goal");
        return u;
    }
}
