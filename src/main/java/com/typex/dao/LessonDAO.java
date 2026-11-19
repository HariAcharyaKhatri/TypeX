package com.typex.dao;

import com.typex.database.DatabaseManager;
import com.typex.model.Lesson;

import java.sql.*;
import java.util.*;

public class LessonDAO {
    public List<Lesson> findAll() {
        List<Lesson> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM lessons ORDER BY level, order_index")) {
                while (rs.next()) out.add(map(rs));
            } catch (SQLException e) {
                System.err.println("Error fetching lessons: " + e.getMessage());
            }
        }
        return out;
    }

    public List<Lesson> findByLevel(int level) {
        List<Lesson> out = new ArrayList<>();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM lessons WHERE level=? ORDER BY order_index")) {
                ps.setInt(1, level);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching lessons by level: " + e.getMessage());
            }
        }
        return out;
    }

    public Lesson findById(int id) {
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM lessons WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            } catch (SQLException e) {
                System.err.println("Error finding lesson: " + e.getMessage());
                return null;
            }
        }
    }

    private Lesson map(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.id = rs.getInt("id");
        l.categoryId = rs.getInt("category_id");
        l.level = rs.getInt("level");
        l.orderIndex = rs.getInt("order_index");
        l.title = rs.getString("title");
        l.objective = rs.getString("objective");
        l.instructions = rs.getString("instructions");
        l.practiceText = rs.getString("practice_text");
        l.minWpm = rs.getDouble("min_wpm");
        l.minAccuracy = rs.getDouble("min_accuracy");
        return l;
    }
}
