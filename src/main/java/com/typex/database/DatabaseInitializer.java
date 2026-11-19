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

                // Indexes
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_user ON typing_test_results(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_date ON typing_test_results(completed_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_progress_user ON lesson_progress(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_daily_user_date ON daily_statistics(user_id, date)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_attempts_user_lesson ON lesson_attempts(user_id, lesson_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_char_errors_user ON character_errors(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_keyboard_user ON keyboard_sessions(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_pb_user_cat ON personal_bests(user_id, category, duration_seconds)");
            }

            // Migrations: Alter tables to add new Phase 2 columns if not present
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
    }

    private static void seedLessons(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM lesson_categories")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Ensure lessons have non-zero min_wpm and min_accuracy
                try (Statement upd = c.createStatement()) {
                    upd.executeUpdate("UPDATE lessons SET min_wpm=15, min_accuracy=90 WHERE level=1 AND min_wpm=0");
                    upd.executeUpdate("UPDATE lessons SET min_wpm=25, min_accuracy=92 WHERE level=2 AND min_wpm=0");
                    upd.executeUpdate("UPDATE lessons SET min_wpm=35, min_accuracy=94 WHERE level=3 AND min_wpm=0");
                    upd.executeUpdate("UPDATE lessons SET min_wpm=45, min_accuracy=95 WHERE level=4 AND min_wpm=0");
                    upd.executeUpdate("UPDATE lessons SET min_wpm=60, min_accuracy=97 WHERE level=5 AND min_wpm=0");
                }
                return;
            }
        }

        String[][] categories = {
                {"Beginner", "Home row and finger positioning"},
                {"Elementary", "Upper/lower rows and common words"},
                {"Intermediate", "Capital letters, numbers, punctuation"},
                {"Advanced", "Long paragraphs and complex words"},
                {"Professional", "Business documents and high-speed drills"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO lesson_categories(name,description) VALUES(?,?)")) {
            for (String[] cat : categories) {
                ps.setString(1, cat[0]);
                ps.setString(2, cat[1]);
                ps.executeUpdate();
            }
        }

        // {title, objective, instructions, practice_text, min_wpm, min_accuracy}
        Object[][] lessons = {
            // Level 1: Beginner
            {"F and J Positioning", "Locate F and J home anchor keys by feel",
             "Place left index on F (feel the bump), right index on J. Keep hands relaxed.",
             "fff jjj fff jjj fj fj jf jf ff jj fj fff jjj", 15.0, 90.0},
            {"Home Row Mastery", "Master ASDF and JKL; home keys",
             "Rest fingers on ASDF JKL;. Strike keys smoothly without moving hands off the row.",
             "asdf jkl; asdf jkl; sad lad fad ask all fall glad flash", 15.0, 90.0},
            {"Left-Hand Drills", "Develop accuracy with left-hand fingers",
             "Use little finger for A, ring for S, middle for D, index for F.",
             "as as df df sad dad fad ask all fall staff daffy", 15.0, 90.0},
            {"Right-Hand Drills", "Develop accuracy with right-hand fingers",
             "Use index for J, middle for K, ring for L, little finger for semicolon.",
             "jk jk kl kl lol lkj jkl; fall hall silk skill flasks", 15.0, 90.0},

            // Level 2: Elementary
            {"Upper Row: QWERTY", "Reach up from home row to the top letter row",
             "Reach fingers up to QWERTYUIOP and return immediately to home position.",
             "qwer uiop qwer uiop quit type power tower wire root equip", 25.0, 92.0},
            {"Lower Row: ZXCV", "Reach down from home row to the bottom letter row",
             "Reach fingers down to ZXCVBNM and return to home row.",
             "zxcv bnm, zxcv bnm, maze vex car zoom van cabin mixed zinc", 25.0, 92.0},
            {"Shift-Key Practice", "Master capitalization with opposite Shift keys",
             "Press Shift with opposite pinky. Do not lift wrists while capitalizing.",
             "The Quick Brown Fox Jumps Over The Lazy Dog", 25.0, 92.0},
            {"Common Words", "Type high-frequency English vocabulary seamlessly",
             "Maintain smooth rhythm across word transitions.",
             "the and for you that with this have from they which one were", 25.0, 92.0},

            // Level 3: Intermediate
            {"Number Row Drills", "Type the top number row without looking down",
             "Reach fingers up to the number keys: 1-5 with left hand, 6-0 with right hand.",
             "12345 67890 2024 100 42 789 31415 9265 3589", 35.0, 94.0},
            {"Punctuation Precision", "Master commas, periods, question marks, and apostrophes",
             "Keep wrists still. Tap punctuation cleanly with right hand fingers.",
             "Hello, World! How are you? I'm fine. That's great, isn't it?", 35.0, 94.0},
            {"Symbols & Special Characters", "Use Shift with numbers to type common symbols",
             "Hold Shift firmly while striking symbol keys: !, @, #, $, %, &, *.",
             "Save 25% on items #1 & #2: total is $45.00 (tax incl @ 8%).", 35.0, 94.0},
            {"Mixed Alphanumeric", "Combine letters, numbers, and symbols smoothly",
             "Balance speed with zero-error precision across diverse character sets.",
             "User #90210 paid $1,450.75 on 10/15/2024 via Order-ID #TX-778.", 35.0, 94.0},

            // Level 4: Advanced
            {"Sentence Fluency", "Build natural sentence cadence and rhythm",
             "Read ahead one word while typing the current word for unbroken flow.",
             "Consistency beats raw speed. Smooth typing develops effortless muscle memory.", 45.0, 95.0},
            {"Complex Vocabulary", "Navigate polysyllabic and tricky words",
             "Mentally chunk long words into familiar letter sequences.",
             "Extraordinary phenomenon juxtaposition vocabulary consistency indispensable", 45.0, 95.0},
            {"Paragraph Endurance", "Sustain high accuracy over longer passages",
             "Breathe steadily and pace yourself without rushing.",
             "Typing is a cognitive skill that improves with deliberate practice. Focus strictly on accuracy first; speed naturally follows when errors decrease.", 45.0, 95.0},
            {"Speed Building Bursts", "Push your words-per-minute threshold",
             "Allow your fingers to glide quickly across familiar patterns.",
             "The rain in Spain stays mainly in the plain. Quick nimble fingers fly over keys.", 45.0, 95.0},

            // Level 5: Professional
            {"Business Correspondence", "Type formal professional communications",
             "Format formal letters with clean punctuation and capitalization.",
             "Dear Board Members, please review the attached Q3 financial audit before Friday.", 60.0, 97.0},
            {"Technical Documentation", "Format technical code and engineering notes",
             "Maintain sharp attention to brackets, semicolons, and syntax.",
             "function calculateMetric(data) { return data.filter(x => x > 0).reduce((a, b) => a + b, 0); }", 60.0, 97.0},
            {"High-Speed Sprint", "Peak performance speed drill under time constraints",
             "Let muscle memory take over completely. Trust your finger positioning.",
             "knowledge creates capability and deliberate practice transforms capability into mastery.", 60.0, 97.0},
            {"Zero-Defect Challenge", "Flawless typing test with zero mistakes allowed",
             "Every single keystroke must land accurately on its designated target.",
             "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away.", 60.0, 98.0}
        };

        int[] levelCatIds = new int[6];
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM lesson_categories ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                int i = 1;
                while (rs.next() && i < levelCatIds.length) {
                    levelCatIds[i++] = rs.getInt(1);
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO lessons(category_id,level,order_index,title,objective,instructions,practice_text,min_wpm,min_accuracy) " +
                "VALUES(?,?,?,?,?,?,?,?,?)")) {
            int level = 1;
            int order = 1;
            for (Object[] l : lessons) {
                ps.setInt(1, levelCatIds[level]);
                ps.setInt(2, level);
                ps.setInt(3, order);
                ps.setString(4, (String) l[0]);
                ps.setString(5, (String) l[1]);
                ps.setString(6, (String) l[2]);
                ps.setString(7, (String) l[3]);
                ps.setDouble(8, (Double) l[4]);
                ps.setDouble(9, (Double) l[5]);
                ps.executeUpdate();
                order++;
                if (order > 4) {
                    level++;
                    order = 1;
                }
            }
        }
    }

    private static void seedAchievements(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM achievements")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        Object[][] rows = {
                {"FIRST_TEST", "First Test", "Complete your first typing test", 50, "🏁"},
                {"FIRST_LESSON", "First Lesson", "Complete your first lesson", 50, "📚"},
                {"WPM_20", "20 WPM Club", "Reach 20 WPM in a test", 100, "🥉"},
                {"WPM_30", "30 WPM Club", "Reach 30 WPM", 150, "🥈"},
                {"WPM_40", "40 WPM Club", "Reach 40 WPM", 200, "🥇"},
                {"WPM_50", "50 WPM Club", "Reach 50 WPM", 250, "🏅"},
                {"WPM_70", "70 WPM Club", "Reach 70 WPM", 400, "🎖"},
                {"WPM_100", "100 WPM Club", "Reach 100 WPM", 600, "🏆"},
                {"PERFECT_ACCURACY", "Perfect Accuracy", "Complete a test with 100% accuracy", 200, "💎"},
                {"LESSONS_10", "10 Lessons Completed", "Complete 10 lessons", 200, "📖"},
                {"LESSONS_ALL", "Master Typist", "Complete all 20 curriculum lessons", 1000, "🎓"},
                {"TESTS_100", "100 Tests Completed", "Complete 100 tests", 500, "🎯"},
                {"STREAK_7", "7-Day Streak", "Practice 7 days in a row", 150, "🔥"},
                {"STREAK_30", "30-Day Streak", "Practice 30 days in a row", 400, "🔥"},
                {"CONSISTENCY_90", "Rhythm Master", "Reach 90% typing consistency in a test", 250, "⚡"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO achievements(code,title,description,xp,icon) VALUES(?,?,?,?,?)")) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setInt(4, (Integer) r[3]);
                ps.setString(5, (String) r[4]);
                ps.executeUpdate();
            }
        }
    }
}
