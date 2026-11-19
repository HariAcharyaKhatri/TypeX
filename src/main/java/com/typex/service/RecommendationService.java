package com.typex.service;

import com.typex.dao.CharacterErrorDAO;
import com.typex.dao.LessonDAO;
import com.typex.dao.TypingResultDAO;
import com.typex.model.CharacterError;
import com.typex.model.Lesson;
import com.typex.model.PracticeRecommendation;
import com.typex.model.TypingResult;

import java.util.ArrayList;
import java.util.List;

public class RecommendationService {
    private final CharacterErrorDAO charErrors = new CharacterErrorDAO();
    private final LessonDAO lessonDao = new LessonDAO();
    private final LessonService lessonService = new LessonService();
    private final TypingResultDAO resultsDao = new TypingResultDAO();

    public List<PracticeRecommendation> getRecommendations(int userId) {
        List<PracticeRecommendation> recs = new ArrayList<>();

        // 1. Weak-Key Recommendation
        List<CharacterError> topErrors = charErrors.getTopErrors(userId, 3);
        if (topErrors != null && !topErrors.isEmpty() && topErrors.get(0).errorCount >= 2) {
            StringBuilder sb = new StringBuilder();
            int totalErr = 0;
            for (int i = 0; i < topErrors.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("'").append(topErrors.get(i).charDisplay).append("'");
                totalErr += topErrors.get(i).errorCount;
            }
            recs.add(new PracticeRecommendation(
                    PracticeRecommendation.Type.WEAK_KEYS,
                    "Targeted Key Drill",
                    "Focus specifically on keys " + sb + " to eliminate finger hesitation.",
                    "You mistyped these keys " + totalErr + " times recently.",
                    "keyboard",
                    "Practice Weak Keys"
            ));
        }

        // 2. Curriculum Lesson Recommendation
        List<Lesson> allLessons = lessonDao.findAll();
        Lesson nextLesson = null;
        for (Lesson l : allLessons) {
            if (!lessonService.isCompleted(userId, l.id)) {
                nextLesson = l;
                break;
            }
        }
        if (nextLesson != null) {
            PracticeRecommendation rec = new PracticeRecommendation(
                    PracticeRecommendation.Type.NEXT_LESSON,
                    "Continue Curriculum: " + nextLesson.title,
                    nextLesson.objective != null ? nextLesson.objective : "Advance to the next milestone in your typing curriculum.",
                    "Level " + nextLesson.level + " lesson is unlocked and waiting for you.",
                    "learn",
                    "Start Lesson"
            );
            rec.extraData = nextLesson.id;
            recs.add(rec);
        }

        // 3. Accuracy vs Speed Recommendation
        List<TypingResult> recent = resultsDao.findByUser(userId, 5);
        if (!recent.isEmpty()) {
            double avgAcc = recent.stream().mapToDouble(r -> r.accuracy).average().orElse(100.0);
            if (avgAcc < 94.0) {
                recs.add(new PracticeRecommendation(
                        PracticeRecommendation.Type.ACCURACY_BOOSTER,
                        "Precision Booster Drill",
                        "Slow down by 10-15% and focus on 100% clean keystrokes.",
                        String.format("Recent accuracy is %.1f%%. High accuracy develops effortless speed.", avgAcc),
                        "test",
                        "Practice Precision"
                ));
            } else {
                double avgWpm = recent.stream().mapToDouble(r -> r.netWpm).average().orElse(30.0);
                recs.add(new PracticeRecommendation(
                        PracticeRecommendation.Type.SPEED_SPRINT,
                        "Speed Burst Challenge",
                        "Push your fingers to sprint ahead without pausing on known patterns.",
                        String.format("Great accuracy (%.1f%%)! Aim for %.0f+ WPM on a 30s sprint.", avgAcc, avgWpm + 5),
                        "test",
                        "Start Sprint"
                ));
            }
        } else {
            // First time user
            recs.add(new PracticeRecommendation(
                    PracticeRecommendation.Type.SPEED_SPRINT,
                    "Take Benchmark Test",
                    "Complete your initial 60-second typing test to calibrate your personal metrics.",
                    "Establishes your baseline speed and accuracy.",
                    "test",
                    "Take Benchmark"
            ));
        }

        return recs;
    }
}
