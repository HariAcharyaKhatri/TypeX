package com.typex.database;

import java.sql.*;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialize() throws SQLException {
        Connection c = DatabaseManager.getInstance().getConnection();
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
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_user ON typing_test_results(user_id)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_results_date ON typing_test_results(completed_at)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_progress_user ON lesson_progress(user_id)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_daily_user_date ON daily_statistics(user_id, date)");
        }
        seedLessons(c);
        seedAchievements(c);
    }

    private static void seedLessons(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM lesson_categories")) {
            if (rs.next() && rs.getInt(1) > 0) return;
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
        String[][] lessonsByLevel = {
            // level 1
            {"F and J positioning", "Locate the F and J home keys by feel", "Place left index on F, right index on J.",
             "fff jjj fff jjj fj fj jf jf ff jj fj"},
            {"Home row", "Master ASDF JKL;", "Keep fingers on the home row. Return after each key.",
             "asdf jkl; asdf jkl; sad lad fad ask all fall"},
            {"Left-hand letters", "Practice ASDF letters", "Use the correct finger for each key.",
             "as as df df sad dad fad ask all fall"},
            {"Right-hand letters", "Practice JKL; letters", "Use the correct finger for each key.",
             "jk jk kl kl lol lkj jkl; fall hall"},
            // level 2
            {"Upper row: QWERTY", "Introduce the upper row letters", "Reach up from the home row.",
             "qwer uiop qwer uiop quit type power tower"},
            {"Common words", "Type frequent English words", "Rhythm matters more than speed.",
             "the and for you that with this have from they"},
            {"Short sentences", "Type complete sentences", "Capitalize nothing, no punctuation yet.",
             "the quick brown fox jumps over the lazy dog"},
            {"Lower row: ZXCV", "Introduce the lower row letters", "Reach down from the home row.",
             "zxcv bnm, zxcv bnm, maze vex car zoom van"},
            // level 3
            {"Capital letters", "Master the Shift key", "Hold Shift with the opposite pinky.",
             "The Quick Brown Fox Jumps Over The Lazy Dog"},
            {"Numbers", "Type the number row", "Reach without looking down.",
             "1234567890 0987654321 42 100 2024"},
            {"Punctuation", "Commas, periods, and more", "Keep rhythm steady.",
             "Hello, World! How are you? I'm fine. Thanks."},
            {"Symbols", "Special characters", "Shift for symbols.",
             "The price is $42.50 (approx). Use 50% off @ store!"},
            // level 4
            {"Paragraph practice", "Type a long paragraph", "Maintain accuracy while typing.",
             "Typing is a skill that improves with deliberate practice. Focus on accuracy first, then speed."},
            {"Complex words", "Tricky vocabulary", "Break words mentally into chunks.",
             "Extraordinary phenomenon juxtaposition vocabulary consistency extraordinary"},
            {"Numbers and symbols", "Mixed alphanumeric content", "Balance speed and precision.",
             "Invoice #A42-7X costs $1,299.99 with 15% tax and 3.5% shipping."},
            {"Speed building", "Push your limits", "Aim for smooth bursts.",
             "The rain in Spain stays mainly in the plain. She sells seashells by the seashore."},
            // level 5
            {"Business documents", "Professional writing", "Formal business English.",
             "Dear team, please review the attached proposal and submit feedback by Friday, June 14th."},
            {"Long passages", "Sustained typing", "Endurance matters.",
             "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. "
               + "How vexingly quick daft zebras jump! Sphinx of black quartz, judge my vow."},
            {"High-speed drill", "Fast typing under pressure", "Trust your muscle memory.",
             "the and for you that with this have from they which one were her all she there would"},
            {"Accuracy challenge", "Zero mistakes", "Slow down if needed.",
             "Precision beats speed. Every keystroke should land cleanly on the intended letter."}
        };
        // Reopen statement to find category IDs
        int[] levelCatIds = new int[6];
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM lesson_categories ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                int i = 1;
                while (rs.next() && i < levelCatIds.length) levelCatIds[i++] = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO lessons(category_id,level,order_index,title,objective,instructions,practice_text,min_wpm,min_accuracy) " +
                "VALUES(?,?,?,?,?,?,?,?,?)")) {
            int level = 1;
            int order = 1;
            for (String[] l : lessonsByLevel) {
                ps.setInt(1, levelCatIds[level]);
                ps.setInt(2, level);
                ps.setInt(3, order);
                ps.setString(4, l[0]);
                ps.setString(5, l[1]);
                ps.setString(6, l[2]);
                ps.setString(7, l[3]);
                ps.setDouble(8, 0);
                ps.setDouble(9, 0);
                ps.executeUpdate();
                order++;
                if (order > 4) { level++; order = 1; }
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
                {"TESTS_100", "100 Tests Completed", "Complete 100 tests", 500, "🎯"},
                {"STREAK_7", "7-Day Streak", "Practice 7 days in a row", 150, "🔥"},
                {"STREAK_30", "30-Day Streak", "Practice 30 days in a row", 400, "🔥"}
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
