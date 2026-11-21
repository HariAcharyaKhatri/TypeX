package com.typex.database;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialize() throws SQLException {
        Connection c = DatabaseManager.getInstance().getConnection();
        synchronized (DatabaseManager.getInstance().getLock()) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      username TEXT NOT NULL UNIQUE,
                      avatar_path TEXT,
                      language TEXT DEFAULT 'en',
                      skill_level TEXT DEFAULT 'Beginner',
                      daily_goal INTEGER DEFAULT 30,
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                      user_id INTEGER PRIMARY KEY,
                      theme TEXT DEFAULT 'dark',
                      accent_color TEXT DEFAULT '#FFC107',
                      font_size INTEGER DEFAULT 16,
                      sound_enabled INTEGER DEFAULT 1,
                      volume INTEGER DEFAULT 70,
                      default_duration INTEGER DEFAULT 60,
                      show_keyboard INTEGER DEFAULT 1,
                      show_finger_guide INTEGER DEFAULT 1,
                      auto_start INTEGER DEFAULT 1,
                      highlight_correct INTEGER DEFAULT 1,
                      highlight_incorrect INTEGER DEFAULT 1,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lesson_categories (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      name TEXT NOT NULL,
                      description TEXT
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lessons (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      category_id INTEGER NOT NULL,
                      level INTEGER NOT NULL,
                      order_index INTEGER NOT NULL,
                      title TEXT NOT NULL,
                      objective TEXT,
                      instructions TEXT,
                      practice_text TEXT NOT NULL,
                      min_wpm REAL DEFAULT 0,
                      min_accuracy REAL DEFAULT 0,
                      FOREIGN KEY(category_id) REFERENCES lesson_categories(id)
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lesson_progress (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      lesson_id INTEGER NOT NULL,
                      completed INTEGER DEFAULT 0,
                      best_wpm REAL DEFAULT 0,
                      best_accuracy REAL DEFAULT 0,
                      attempts INTEGER DEFAULT 0,
                      last_attempt TEXT,
                      UNIQUE(user_id, lesson_id),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY(lesson_id) REFERENCES lessons(id)
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS typing_test_results (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      category TEXT,
                      duration_seconds INTEGER,
                      gross_wpm REAL,
                      net_wpm REAL,
                      accuracy REAL,
                      correct_chars INTEGER,
                      incorrect_chars INTEGER,
                      total_chars INTEGER,
                      total_keystrokes INTEGER,
                      backspaces INTEGER,
                      error_percent REAL,
                      completed_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS custom_exercises (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      title TEXT NOT NULL,
                      text TEXT NOT NULL,
                      difficulty TEXT,
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS achievements (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      code TEXT NOT NULL UNIQUE,
                      title TEXT NOT NULL,
                      description TEXT,
                      xp INTEGER DEFAULT 0,
                      icon TEXT
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_achievements (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      achievement_id INTEGER NOT NULL,
                      unlocked_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE(user_id, achievement_id),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY(achievement_id) REFERENCES achievements(id)
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS daily_statistics (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      date TEXT NOT NULL,
                      practice_seconds INTEGER DEFAULT 0,
                      words_typed INTEGER DEFAULT 0,
                      tests_completed INTEGER DEFAULT 0,
                      lessons_completed INTEGER DEFAULT 0,
                      avg_wpm REAL DEFAULT 0,
                      avg_accuracy REAL DEFAULT 0,
                      UNIQUE(user_id, date),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                // --- PHASE 2 TABLES ---
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lesson_attempts (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      lesson_id INTEGER NOT NULL,
                      wpm REAL NOT NULL,
                      accuracy REAL NOT NULL,
                      duration_seconds INTEGER DEFAULT 0,
                      completed INTEGER DEFAULT 0,
                      attempted_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY(lesson_id) REFERENCES lessons(id)
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS character_errors (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      char_code INTEGER NOT NULL,
                      char_display TEXT NOT NULL,
                      error_count INTEGER DEFAULT 0,
                      total_attempts INTEGER DEFAULT 0,
                      last_mistyped TEXT DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE(user_id, char_code),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS keyboard_sessions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      drill_type TEXT NOT NULL,
                      keys_tested INTEGER DEFAULT 0,
                      correct_keys INTEGER DEFAULT 0,
                      accuracy REAL DEFAULT 0,
                      avg_reaction_time_ms REAL DEFAULT 0,
                      completed_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS personal_bests (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      category TEXT NOT NULL,
                      duration_seconds INTEGER NOT NULL,
                      best_wpm REAL DEFAULT 0,
                      accuracy REAL DEFAULT 0,
                      achieved_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE(user_id, category, duration_seconds),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                // --- PHASE 3 TABLES ---
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_xp (
                      user_id INTEGER PRIMARY KEY,
                      total_xp INTEGER DEFAULT 0,
                      level INTEGER DEFAULT 1,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS practice_streaks (
                      user_id INTEGER PRIMARY KEY,
                      current_streak INTEGER DEFAULT 0,
                      longest_streak INTEGER DEFAULT 0,
                      last_practice_date TEXT,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS daily_goals (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      date TEXT NOT NULL,
                      target_minutes INTEGER DEFAULT 15,
                      target_tests INTEGER DEFAULT 3,
                      target_lessons INTEGER DEFAULT 1,
                      target_accuracy REAL DEFAULT 95.0,
                      completed_seconds INTEGER DEFAULT 0,
                      completed_tests INTEGER DEFAULT 0,
                      completed_lessons INTEGER DEFAULT 0,
                      achieved INTEGER DEFAULT 0,
                      reward_claimed INTEGER DEFAULT 0,
                      UNIQUE(user_id, date),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS challenges (
                      id TEXT PRIMARY KEY,
                      type TEXT NOT NULL,
                      title TEXT NOT NULL,
                      description TEXT NOT NULL,
                      requirement_type TEXT NOT NULL,
                      target_value REAL NOT NULL,
                      xp_reward INTEGER NOT NULL,
                      start_date TEXT NOT NULL,
                      end_date TEXT NOT NULL
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_challenges (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      challenge_id TEXT NOT NULL,
                      current_progress REAL DEFAULT 0,
                      completed INTEGER DEFAULT 0,
                      reward_claimed INTEGER DEFAULT 0,
                      completed_at TEXT,
                      UNIQUE(user_id, challenge_id),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY(challenge_id) REFERENCES challenges(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_preferences (
                      user_id INTEGER PRIMARY KEY,
                      daily_goal_minutes INTEGER DEFAULT 15,
                      daily_goal_tests INTEGER DEFAULT 3,
                      daily_goal_lessons INTEGER DEFAULT 1,
                      daily_goal_accuracy REAL DEFAULT 95.0,
                      preferred_duration INTEGER DEFAULT 60,
                      preferred_category TEXT DEFAULT 'Common English',
                      sound_enabled INTEGER DEFAULT 1,
                      volume INTEGER DEFAULT 70,
                      level_up_alerts INTEGER DEFAULT 1,
                      achievement_alerts INTEGER DEFAULT 1,
                      keyboard_layout TEXT DEFAULT 'US QWERTY',
                      passage_font_size INTEGER DEFAULT 26,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                // --- PHASE 5 TABLES ---
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS game_sessions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      game_type TEXT NOT NULL,
                      score INTEGER NOT NULL,
                      level_reached INTEGER DEFAULT 1,
                      words_completed INTEGER DEFAULT 0,
                      accuracy REAL DEFAULT 0,
                      wpm REAL DEFAULT 0,
                      duration_seconds INTEGER DEFAULT 0,
                      xp_earned INTEGER DEFAULT 0,
                      played_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS game_personal_bests (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      game_type TEXT NOT NULL,
                      high_score INTEGER DEFAULT 0,
                      highest_level INTEGER DEFAULT 1,
                      best_wpm REAL DEFAULT 0,
                      best_accuracy REAL DEFAULT 0,
                      achieved_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE(user_id, game_type),
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS custom_typing_tests (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      title TEXT NOT NULL,
                      content TEXT NOT NULL,
                      difficulty TEXT DEFAULT 'Normal',
                      target_duration INTEGER DEFAULT 60,
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )""");

                // Indexes
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_user ON typing_test_results(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_date ON typing_test_results(completed_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_progress_user ON lesson_progress(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_daily_user_date ON daily_statistics(user_id, date)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_attempts_user_lesson ON lesson_attempts(user_id, lesson_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_char_errors_user ON character_errors(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_keyboard_user ON keyboard_sessions(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_pb_user_cat ON personal_bests(user_id, category, duration_seconds)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_daily_goals_user_date ON daily_goals(user_id, date)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_user_challenges_user ON user_challenges(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_game_sessions_user ON game_sessions(user_id, game_type)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_game_pb_user ON game_personal_bests(user_id, game_type)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_custom_tests_user ON custom_typing_tests(user_id)");
            }

            // Migrations: Alter tables to add new Phase 2 & 3 columns if not present
            applyMigrations(c);

            // Seed lessons & achievements
            seedLessons(c);
            seedAchievements(c);
        }
    }

    private static void applyMigrations(Connection c) throws SQLException {
        Set<String> resultCols = new HashSet<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(typing_test_results)")) {
            while (rs.next()) {
                resultCols.add(rs.getString("name").toLowerCase());
            }
        }

        try (Statement st = c.createStatement()) {
            if (!resultCols.contains("raw_accuracy")) {
                st.executeUpdate("ALTER TABLE typing_test_results ADD COLUMN raw_accuracy REAL DEFAULT 0");
            }
            if (!resultCols.contains("corrected_errors")) {
                st.executeUpdate("ALTER TABLE typing_test_results ADD COLUMN corrected_errors INTEGER DEFAULT 0");
            }
            if (!resultCols.contains("uncorrected_errors")) {
                st.executeUpdate("ALTER TABLE typing_test_results ADD COLUMN uncorrected_errors INTEGER DEFAULT 0");
            }
            if (!resultCols.contains("consistency")) {
                st.executeUpdate("ALTER TABLE typing_test_results ADD COLUMN consistency REAL DEFAULT 0");
            }
            if (!resultCols.contains("raw_keystrokes")) {
                st.executeUpdate("ALTER TABLE typing_test_results ADD COLUMN raw_keystrokes INTEGER DEFAULT 0");
            }
        }

        // Migration for achievements: category column
        Set<String> achCols = new HashSet<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(achievements)")) {
            while (rs.next()) {
                achCols.add(rs.getString("name").toLowerCase());
            }
        }
        if (!achCols.contains("category")) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("ALTER TABLE achievements ADD COLUMN category TEXT DEFAULT 'General'");
            }
        }

        // Migration for user_preferences: keyboard_layout and passage_font_size
        Set<String> prefCols = new HashSet<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(user_preferences)")) {
            while (rs.next()) {
                prefCols.add(rs.getString("name").toLowerCase());
            }
        }
        if (!prefCols.contains("keyboard_layout")) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("ALTER TABLE user_preferences ADD COLUMN keyboard_layout TEXT DEFAULT 'US QWERTY'");
            }
        }
        if (!prefCols.contains("passage_font_size")) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("ALTER TABLE user_preferences ADD COLUMN passage_font_size INTEGER DEFAULT 26");
            }
        }
    }

    private static void seedLessons(Connection c) throws SQLException {
        // Ensure 5 lesson categories exist
        String[][] categories = {
                {"Beginner", "Home row and finger positioning"},
                {"Elementary", "Upper/lower rows and common words"},
                {"Intermediate", "Capital letters, numbers, punctuation"},
                {"Advanced", "Long paragraphs and complex words"},
                {"Professional", "Business documents and high-speed drills"}
        };
        for (int i = 0; i < categories.length; i++) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO lesson_categories(id, name, description) VALUES(?, ?, ?)")) {
                ps.setInt(1, i + 1);
                ps.setString(2, categories[i][0]);
                ps.setString(3, categories[i][1]);
                ps.executeUpdate();
            }
        }

        // Map level (1-5) to category_id
        int[] levelCatIds = new int[6];
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM lesson_categories ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                int i = 1;
                while (rs.next() && i < levelCatIds.length) {
                    levelCatIds[i++] = rs.getInt(1);
                }
            }
        }

        // Upsert 100 lessons idempotently from LessonCurriculum
        java.util.List<LessonCurriculum.LessonSeed> seeds = LessonCurriculum.getFullCurriculum();
        for (LessonCurriculum.LessonSeed s : seeds) {
            int catId = (s.level >= 1 && s.level < levelCatIds.length) ? levelCatIds[s.level] : 1;
            boolean exists = false;
            try (PreparedStatement chk = c.prepareStatement(
                    "SELECT id FROM lessons WHERE level = ? AND order_index = ?")) {
                chk.setInt(1, s.level);
                chk.setInt(2, s.orderIndex);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (!exists) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO lessons(category_id, level, order_index, title, objective, instructions, practice_text, min_wpm, min_accuracy) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ins.setInt(1, catId);
                    ins.setInt(2, s.level);
                    ins.setInt(3, s.orderIndex);
                    ins.setString(4, s.title);
                    ins.setString(5, s.objective);
                    ins.setString(6, s.instructions);
                    ins.setString(7, s.practiceText);
                    ins.setDouble(8, s.minWpm);
                    ins.setDouble(9, s.minAccuracy);
                    ins.executeUpdate();
                }
            } else {
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE lessons SET title=?, objective=?, instructions=?, practice_text=?, min_wpm=?, min_accuracy=? " +
                        "WHERE level=? AND order_index=?")) {
                    upd.setString(1, s.title);
                    upd.setString(2, s.objective);
                    upd.setString(3, s.instructions);
                    upd.setString(4, s.practiceText);
                    upd.setDouble(5, s.minWpm);
                    upd.setDouble(6, s.minAccuracy);
                    upd.setInt(7, s.level);
                    upd.setInt(8, s.orderIndex);
                    upd.executeUpdate();
                }
            }
        }
    }

    private static void seedAchievements(Connection c) throws SQLException {
        Object[][] rows = {
                // Code, Title, Description, XP, Icon, Category
                // Speed
                {"FIRST_TEST", "First Test", "Complete your first typing test", 50, "🏁", "Speed"},
                {"WPM_20", "20 WPM Club", "Reach 20 WPM in a test", 100, "🥉", "Speed"},
                {"WPM_30", "30 WPM Club", "Reach 30 WPM", 150, "🥈", "Speed"},
                {"WPM_40", "40 WPM Club", "Reach 40 WPM", 200, "🥇", "Speed"},
                {"WPM_50", "50 WPM Club", "Reach 50 WPM", 250, "🏅", "Speed"},
                {"WPM_60", "60 WPM Pro", "Reach 60 WPM in a typing test", 300, "⚡", "Speed"},
                {"WPM_70", "70 WPM Club", "Reach 70 WPM", 400, "🎖", "Speed"},
                {"WPM_80", "80 WPM Elite", "Reach 80 WPM in a typing test", 450, "🚀", "Speed"},
                {"WPM_100", "100 WPM Club", "Reach 100 WPM", 600, "🏆", "Speed"},
                {"WPM_120", "Speed Demon", "Reach 120 WPM in a typing test", 800, "☄️", "Speed"},
                {"TESTS_100", "100 Tests Completed", "Complete 100 tests", 500, "🎯", "Speed"},

                // Accuracy
                {"ACCURACY_95", "Sharp Shooter", "Complete a test with at least 95% accuracy", 100, "🎯", "Accuracy"},
                {"ACCURACY_98", "Sniper Typist", "Complete a test with at least 98% accuracy", 250, "🏹", "Accuracy"},
                {"PERFECT_ACCURACY", "Perfect Accuracy", "Complete a test with 100% accuracy", 200, "💎", "Accuracy"},
                {"ZERO_BACKSPACES", "Flawless Keystrokes", "Complete a test with zero backspaces", 300, "✨", "Accuracy"},

                // Consistency
                {"CONSISTENCY_80", "Steady Hands", "Reach 80% typing consistency in a test", 150, "⏱️", "Consistency"},
                {"CONSISTENCY_90", "Rhythm Master", "Reach 90% typing consistency in a test", 250, "⚡", "Consistency"},
                {"CONSISTENCY_95", "Metronome", "Reach 95% typing consistency in a test", 400, "🎵", "Consistency"},

                // Learning
                {"FIRST_LESSON", "First Lesson", "Complete your first lesson", 50, "📚", "Learning"},
                {"LESSONS_5", "Dedicated Student", "Complete 5 curriculum lessons", 100, "📗", "Learning"},
                {"LESSONS_10", "10 Lessons Completed", "Complete 10 lessons", 200, "📖", "Learning"},
                {"LESSONS_BEGINNER", "Beginner Graduate", "Pass all Level 1 Beginner lessons", 200, "🌱", "Learning"},
                {"LESSONS_PRO", "Virtuoso Graduate", "Pass all Level 5 Professional lessons", 600, "🌟", "Learning"},
                {"LESSONS_ALL", "Master Typist", "Complete all 20 curriculum lessons", 1000, "🎓", "Learning"},

                // Practice Time
                {"TIME_15M", "Warm Up", "Accumulate 15 minutes of total practice time", 50, "⏳", "Practice Time"},
                {"TIME_1H", "Hour of Power", "Accumulate 1 hour of total practice time", 200, "🕰️", "Practice Time"},
                {"TIME_5H", "Iron Fingers", "Accumulate 5 hours of total practice time", 500, "🛡️", "Practice Time"},

                // Streak
                {"STREAK_3", "Habit Former", "Practice 3 days in a row", 75, "🔥", "Streak"},
                {"STREAK_7", "7-Day Streak", "Practice 7 days in a row", 150, "🔥", "Streak"},
                {"STREAK_14", "Two-Week Triumph", "Practice 14 days in a row", 250, "🔥", "Streak"},
                {"STREAK_30", "30-Day Streak", "Practice 30 days in a row", 400, "🔥", "Streak"},

                // Keyboard Mastery
                {"KEYBOARD_FIRST_DRILL", "Touch Explorer", "Complete your first keyboard trainer drill", 50, "⌨️", "Keyboard Mastery"},
                {"KEYBOARD_5_DRILLS", "Keyboard Artisan", "Complete 5 keyboard trainer sessions", 200, "🎹", "Keyboard Mastery"},
                {"ACCURACY_HIGH_SPEED", "Speed & Precision", "Achieve 50+ WPM with 98%+ accuracy", 350, "👑", "Keyboard Mastery"}
        };

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO achievements(code,title,description,xp,icon,category) VALUES(?,?,?,?,?,?)")) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setInt(4, (Integer) r[3]);
                ps.setString(5, (String) r[4]);
                ps.setString(6, (String) r[5]);
                ps.executeUpdate();
            }
        }

        // Update category for any previously inserted achievements
        try (PreparedStatement up = c.prepareStatement(
                "UPDATE achievements SET category=? WHERE code=? AND (category IS NULL OR category='General')")) {
            for (Object[] r : rows) {
                up.setString(1, (String) r[5]);
                up.setString(2, (String) r[0]);
                up.executeUpdate();
            }
        }
    }
}
