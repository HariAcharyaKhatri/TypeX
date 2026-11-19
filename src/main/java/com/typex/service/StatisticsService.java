package com.typex.service;

import com.typex.dao.StatisticsDAO;
import com.typex.dao.TypingResultDAO;
import com.typex.model.DailyStat;
import com.typex.model.TypingResult;

import java.util.Collections;
import java.util.List;

public class StatisticsService {
    private final TypingResultDAO results = new TypingResultDAO();
    private final StatisticsDAO stats = new StatisticsDAO();

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

    public void saveResult(TypingResult r) {
        if (r == null) return;
        results.insert(r);
        stats.recordTest(r.userId, r.netWpm, r.accuracy, r.durationSeconds,
                Math.max(1, r.totalChars / 5));
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
