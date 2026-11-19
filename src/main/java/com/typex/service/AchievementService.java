package com.typex.service;

import com.typex.dao.AchievementDAO;
import com.typex.dao.TypingResultDAO;
import com.typex.model.Achievement;
import com.typex.model.TypingResult;

import java.util.ArrayList;
import java.util.List;

public class AchievementService {
    private final AchievementDAO dao = new AchievementDAO();
    private final TypingResultDAO results = new TypingResultDAO();

    public List<Achievement> forUser(int userId) {
        return dao.allForUser(userId);
    }

    /** Evaluates achievements after a test. Returns newly unlocked achievements. */
    public List<Achievement> evaluateAfterTest(int userId, TypingResult r, int totalTests) {
        List<Achievement> newly = new ArrayList<>();
        if (r == null) return newly;
        check(userId, "FIRST_TEST", newly);
        if (r.netWpm >= 20) check(userId, "WPM_20", newly);
        if (r.netWpm >= 30) check(userId, "WPM_30", newly);
        if (r.netWpm >= 40) check(userId, "WPM_40", newly);
        if (r.netWpm >= 50) check(userId, "WPM_50", newly);
        if (r.netWpm >= 70) check(userId, "WPM_70", newly);
        if (r.netWpm >= 100) check(userId, "WPM_100", newly);
        if (r.accuracy >= 100.0) check(userId, "PERFECT_ACCURACY", newly);
        if (totalTests >= 100) check(userId, "TESTS_100", newly);
        return newly;
    }

    public List<Achievement> evaluateAfterLesson(int userId, int lessonsCompleted) {
        List<Achievement> newly = new ArrayList<>();
        check(userId, "FIRST_LESSON", newly);
        if (lessonsCompleted >= 10) check(userId, "LESSONS_10", newly);
        return newly;
    }

    public List<Achievement> evaluateStreak(int userId, int streak) {
        List<Achievement> newly = new ArrayList<>();
        if (streak >= 7) check(userId, "STREAK_7", newly);
        if (streak >= 30) check(userId, "STREAK_30", newly);
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
