package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.CustomExercise;

import java.sql.*;
import java.util.*;

public class CustomExerciseDAO {
    public List<CustomExercise> findByUser(int userId) {
        List<CustomExercise> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM custom_exercises WHERE user_id=? ORDER BY created_at DESC")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CustomExercise e = new CustomExercise();
                        e.id = rs.getInt("id");
                        e.userId = rs.getInt("user_id");
                        e.title = rs.getString("title");
                        e.text = rs.getString("text");
                        e.difficulty = rs.getString("difficulty");
                        out.add(e);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error fetching custom exercises: " + ex.getMessage());
            }
        }
        return out;
    }

    public CustomExercise insert(CustomExercise e) {
        if (e == null) return null;
        String title = (e.title != null ? e.title.trim() : "Untitled");
        if (title.length() > 100) title = title.substring(0, 100);
        String text = (e.text != null ? e.text.trim() : "");
        if (text.length() > 25000) text = text.substring(0, 25000);
        String difficulty = (e.difficulty != null ? e.difficulty.trim() : "Custom");

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO custom_exercises(user_id,title,text,difficulty) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, e.userId);
                ps.setString(2, title);
                ps.setString(3, text);
                ps.setString(4, difficulty);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        e.id = rs.getInt(1);
                        e.title = title;
                        e.text = text;
                        e.difficulty = difficulty;
                        return e;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error inserting custom exercise: " + ex.getMessage());
            }
        }
        return e;
    }

    public void update(CustomExercise e) {
        if (e == null) return;
        String title = (e.title != null ? e.title.trim() : "Untitled");
        if (title.length() > 100) title = title.substring(0, 100);
        String text = (e.text != null ? e.text.trim() : "");
        if (text.length() > 25000) text = text.substring(0, 25000);
        String difficulty = (e.difficulty != null ? e.difficulty.trim() : "Custom");

        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "UPDATE custom_exercises SET title=?,text=?,difficulty=? WHERE id=?")) {
                ps.setString(1, title);
                ps.setString(2, text);
                ps.setString(3, difficulty);
                ps.setInt(4, e.id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                System.err.println("Error updating custom exercise: " + ex.getMessage());
            }
        }
    }

    public void delete(int id) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM custom_exercises WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                System.err.println("Error deleting custom exercise: " + ex.getMessage());
            }
        }
    }
}
