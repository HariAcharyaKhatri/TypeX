package com.typex;

import com.typex.dao.CharacterErrorDAO;
import com.typex.dao.TypingResultDAO;
import com.typex.dao.UserDAO;
import com.typex.database.DatabaseInitializer;
import com.typex.model.PracticeRecommendation;
import com.typex.model.TypingResult;
import com.typex.model.User;
import com.typex.service.RecommendationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationServiceTest {
    private static UserDAO userDAO;
    private static RecommendationService recService;
    private static CharacterErrorDAO errorDAO;
    private static TypingResultDAO resultDAO;
    private static User testUser;

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("java.awt.headless", "true");
        DatabaseInitializer.initialize();
        userDAO = new UserDAO();
        recService = new RecommendationService();
        errorDAO = new CharacterErrorDAO();
        resultDAO = new TypingResultDAO();
    }

    @BeforeEach
    public void createTestUser() {
        String uname = "rec_test_" + System.currentTimeMillis();
        testUser = userDAO.create(uname);
        assertNotNull(testUser);
    }

    @Test
    public void testWeakKeysRecommendation() {
        Map<Character, Integer> errors = new HashMap<>();
        errors.put('z', 6);
        errors.put('q', 4);
        errorDAO.recordErrors(testUser.id, errors);

        List<PracticeRecommendation> recs = recService.getRecommendations(testUser.id);
        assertNotNull(recs);
        assertFalse(recs.isEmpty());

        boolean hasWeakKeyRec = recs.stream().anyMatch(r -> r.type == PracticeRecommendation.Type.WEAK_KEYS);
        assertTrue(hasWeakKeyRec, "Expected a weak-key recommendation when character errors are recorded");
    }

    @Test
    public void testCurriculumLessonRecommendation() {
        List<PracticeRecommendation> recs = recService.getRecommendations(testUser.id);
        boolean hasLessonRec = recs.stream().anyMatch(r -> r.type == PracticeRecommendation.Type.NEXT_LESSON);
        assertTrue(hasLessonRec, "Expected next lesson recommendation for user with incomplete curriculum");
    }

    @Test
    public void testAccuracyBoosterRecommendation() {
        // Record test with lower accuracy (e.g. 88%)
        TypingResult r = new TypingResult();
        r.userId = testUser.id;
        r.durationSeconds = 60;
        r.netWpm = 45.0;
        r.accuracy = 88.0;
        r.totalKeystrokes = 100;
        resultDAO.insert(r);

        List<PracticeRecommendation> recs = recService.getRecommendations(testUser.id);
        boolean hasAccuracyRec = recs.stream().anyMatch(r1 -> r1.type == PracticeRecommendation.Type.ACCURACY_BOOSTER);
        assertTrue(hasAccuracyRec, "Expected accuracy booster recommendation when recent accuracy is below 94%");
    }

    @Test
    public void testSpeedSprintRecommendation() {
        // Record test with high accuracy (e.g. 98%)
        TypingResult r = new TypingResult();
        r.userId = testUser.id;
        r.durationSeconds = 60;
        r.netWpm = 50.0;
        r.accuracy = 98.0;
        r.totalKeystrokes = 150;
        resultDAO.insert(r);

        List<PracticeRecommendation> recs = recService.getRecommendations(testUser.id);
        boolean hasSpeedRec = recs.stream().anyMatch(r1 -> r1.type == PracticeRecommendation.Type.SPEED_SPRINT);
        assertTrue(hasSpeedRec, "Expected speed sprint recommendation when recent accuracy is high");
    }
}
