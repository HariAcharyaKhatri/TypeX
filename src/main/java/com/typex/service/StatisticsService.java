package com.typex.service;

import com.typex.dao.CharacterErrorDAO;
import com.typex.dao.PersonalBestDAO;
import com.typex.dao.StatisticsDAO;
import com.typex.dao.TypingResultDAO;
import com.typex.model.CharacterError;
import com.typex.model.DailyStat;
import com.typex.model.PersonalBest;
import com.typex.model.TypingResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final TypingResultDAO results = new TypingResultDAO();
    private final StatisticsDAO stats = new StatisticsDAO();
    private final CharacterErrorDAO charErrors = new CharacterErrorDAO();
    private final PersonalBestDAO personalBests = new PersonalBestDAO();

    public static double grossWpm(int chars, double minutes) {
        if (minutes <= 0.0 || chars <= 0) return 0.0;
        return (chars / 5.0) / minutes;
    }

    public static double netWpm(int chars, int errors, double minutes) {
        if (minutes <= 0.0) return 0.0;
        double netChars = Math.max(0.0, chars - Math.max(0, errors));
        return (netChars / 5.0) / minutes;
    }

    public static double accuracy(int correct, int total) {
        if (total <= 0) return 100.0;
        if (correct <= 0) return 0.0;
        double acc = (correct * 100.0) / total;
        return Math.max(0.0, Math.min(100.0, acc));
    }

    public boolean saveResult(TypingResult r, Map<Character, Integer> historicalErrors) {
        if (r == null) return false;
        results.insert(r);
        stats.recordTest(r.userId, r.netWpm, r.accuracy, r.durationSeconds,
                Math.max(1, r.totalChars / 5));

        // Persist character errors in batch at end of session (Module 6)
        if (historicalErrors != null && !historicalErrors.isEmpty()) {
            charErrors.recordErrors(r.userId, historicalErrors);
        }

        // Record candidate personal best
        return personalBests.recordCandidate(r.userId, r.category, r.durationSeconds, r.netWpm, r.accuracy);
    }

    public PersonalBest getPersonalBest(int userId, String category, int durationSeconds) {
        return personalBests.getBest(userId, category, durationSeconds);
    }

    public List<PersonalBest> getAllPersonalBests(int userId) {
        return personalBests.getAllBest(userId);
    }

    public List<CharacterError> getTopErrors(int userId, int limit) {
        return charErrors.getTopErrors(userId, limit);
    }

    public Map<Character, Integer> getCharacterErrorMap(int userId) {
        return charErrors.getErrorCountMap(userId);
    }

    public TypingResult bestFor(int userId) {
        return results.findBestByUser(userId);
    }

    public List<TypingResult> recentFor(int userId) {
        return results.findByUser(userId, 20);
    }

    public List<DailyStat> lastDays(int userId, int days) {
        return stats.recent(userId, days);
    }

    public int testCount(int userId) {
        return results.countByUser(userId);
    }

    public long totalPracticeSeconds(int userId) {
        return stats.totalPracticeSeconds(userId);
    }

    public int streak(int userId) {
        return stats.currentStreak(userId);
    }
}
