package com.typex.service;

import com.typex.dao.AchievementDAO;
import com.typex.model.Achievement;
import com.typex.model.TypingResult;

import java.util.ArrayList;
import java.util.List;

public class AchievementService {
    private final AchievementDAO dao = new AchievementDAO();

    public List<Achievement> forUser(int userId) {
        return dao.allForUser(userId);
    }

    /** Evaluates achievements after a test. Returns newly unlocked achievements. */
    public List<Achievement> evaluateAfterTest(int userId, TypingResult r, int totalTests, long totalPracticeSec) {
        List<Achievement> newly = new ArrayList<>();
        if (r == null) return newly;

        // Speed
        check(userId, "FIRST_TEST", newly);
        if (r.netWpm >= 20) check(userId, "WPM_20", newly);
        if (r.netWpm >= 30) check(userId, "WPM_30", newly);
        if (r.netWpm >= 40) check(userId, "WPM_40", newly);
        if (r.netWpm >= 50) check(userId, "WPM_50", newly);
        if (r.netWpm >= 60) check(userId, "WPM_60", newly);
        if (r.netWpm >= 70) check(userId, "WPM_70", newly);
        if (r.netWpm >= 80) check(userId, "WPM_80", newly);
        if (r.netWpm >= 100) check(userId, "WPM_100", newly);
        if (r.netWpm >= 120) check(userId, "WPM_120", newly);
        if (totalTests >= 100) check(userId, "TESTS_100", newly);

        // Accuracy
        if (r.accuracy >= 95.0) check(userId, "ACCURACY_95", newly);
        if (r.accuracy >= 98.0) check(userId, "ACCURACY_98", newly);
        if (r.accuracy >= 100.0) check(userId, "PERFECT_ACCURACY", newly);
        if (r.backspaces == 0 && r.durationSeconds >= 15 && r.totalKeystrokes >= 20) {
            check(userId, "ZERO_BACKSPACES", newly);
        }

        // Consistency
        if (r.consistency >= 80.0) check(userId, "CONSISTENCY_80", newly);
        if (r.consistency >= 90.0) check(userId, "CONSISTENCY_90", newly);
        if (r.consistency >= 95.0) check(userId, "CONSISTENCY_95", newly);

        // Practice Time
        if (totalPracticeSec >= 900) check(userId, "TIME_15M", newly);
        if (totalPracticeSec >= 3600) check(userId, "TIME_1H", newly);
        if (totalPracticeSec >= 18000) check(userId, "TIME_5H", newly);

        // Keyboard Mastery
        if (r.netWpm >= 50.0 && r.accuracy >= 98.0) check(userId, "ACCURACY_HIGH_SPEED", newly);

        return newly;
    }

    public List<Achievement> evaluateAfterLesson(int userId, int lessonsCompleted, boolean beginnerDone, boolean proDone) {
        List<Achievement> newly = new ArrayList<>();
        check(userId, "FIRST_LESSON", newly);
        if (lessonsCompleted >= 5) check(userId, "LESSONS_5", newly);
        if (lessonsCompleted >= 10) check(userId, "LESSONS_10", newly);
        if (beginnerDone) check(userId, "LESSONS_BEGINNER", newly);
        if (proDone) check(userId, "LESSONS_PRO", newly);
        if (lessonsCompleted >= 20) check(userId, "LESSONS_ALL", newly);
        return newly;
    }

    public List<Achievement> evaluateStreak(int userId, int streak) {
        List<Achievement> newly = new ArrayList<>();
        if (streak >= 3) check(userId, "STREAK_3", newly);
        if (streak >= 7) check(userId, "STREAK_7", newly);
        if (streak >= 14) check(userId, "STREAK_14", newly);
        if (streak >= 30) check(userId, "STREAK_30", newly);
        return newly;
    }

    public List<Achievement> evaluateKeyboardDrills(int userId, int completedDrills) {
        List<Achievement> newly = new ArrayList<>();
        if (completedDrills >= 1) check(userId, "KEYBOARD_FIRST_DRILL", newly);
        if (completedDrills >= 5) check(userId, "KEYBOARD_5_DRILLS", newly);
        return newly;
    }

    private void check(int userId, String code, List<Achievement> out) {
        if (dao.unlock(userId, code)) {
            Achievement a = dao.findByCode(code);
            if (a != null) {
                a.unlocked = true;
                out.add(a);
            }
        }
    }
}
