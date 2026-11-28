package com.typex;

import com.typex.dao.CustomExerciseDAO;
import com.typex.dao.GameDAO;
import com.typex.dao.LessonDAO;
import com.typex.dao.UserDAO;
import com.typex.database.DatabaseInitializer;
import com.typex.database.LessonCurriculum;
import com.typex.model.*;
import com.typex.service.GamificationService;
import com.typex.util.IconManager;
import com.typex.util.TextGenerator;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5ExpansionTest {

    private static java.nio.file.Path isolatedDb;
    private static User testUser;
    private static final UserDAO userDAO = new UserDAO();
    private static final LessonDAO lessonDAO = new LessonDAO();
    private static final GameDAO gameDAO = new GameDAO();
    private static final CustomExerciseDAO customDAO = new CustomExerciseDAO();
    private static final GamificationService gamification = new GamificationService();

    @BeforeAll
    static void setup() throws SQLException {
        isolatedDb = com.typex.testutil.TestDatabaseHelper.initIsolatedDatabase();
        String testUsername = com.typex.testutil.TestDatabaseHelper.uniqueUsername("Phase5Tester");
        testUser = userDAO.create(testUsername);
        assertNotNull(testUser, "Test user must be initialized");
    }

    @AfterAll
    static void tearDown() {
        com.typex.testutil.TestDatabaseHelper.teardownIsolatedDatabase(isolatedDb);
    }

    @Test
    @Order(1)
    void testIconManagerVectorRendering() {
        String[] iconKeys = {
            IconManager.DASHBOARD, IconManager.TYPING_TEST, IconManager.LEARN, IconManager.GAMES,
            IconManager.KEYBOARD, IconManager.STATISTICS, IconManager.CUSTOM, IconManager.CHALLENGE,
            IconManager.ACHIEVEMENTS, IconManager.SETTINGS, IconManager.ABOUT, IconManager.PROFILE,
            IconManager.PLAY, IconManager.PAUSE, IconManager.RESTART, IconManager.EXIT,
            IconManager.CHECK, IconManager.LOCK, IconManager.UNLOCK, IconManager.FLAME,
            IconManager.STAR, IconManager.TIMER, IconManager.SPEED, IconManager.ACCURACY,
            IconManager.ROCKET, IconManager.ALIEN, IconManager.CODE, IconManager.HASH,
            IconManager.TARGET, IconManager.TROPHY, IconManager.HEART
        };

        for (String key : iconKeys) {
            Icon icon = IconManager.getIcon(key, 24, Color.BLUE);
            assertNotNull(icon, "Vector icon should not be null for key: " + key);
            assertEquals(24, icon.getIconWidth());
            assertEquals(24, icon.getIconHeight());

            // Paint on buffered image to ensure zero rendering errors
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            assertDoesNotThrow(() -> icon.paintIcon(null, g2, 0, 0));
            g2.dispose();
        }

        // Test application icons
        List<Image> appIcons = IconManager.getAppIconImages();
        assertNotNull(appIcons);
        assertFalse(appIcons.isEmpty());
        assertTrue(appIcons.size() >= 4, "Should provide multi-resolution app icons");
    }

    @Test
    @Order(2)
    void testCurriculum100LessonsSeeded() {
        List<LessonCurriculum.LessonSeed> curriculum = LessonCurriculum.getFullCurriculum();
        assertEquals(100, curriculum.size(), "Curriculum must contain exactly 100 lessons");

        // Verify each level 1 through 5 has exactly 20 lessons
        for (int lvl = 1; lvl <= 5; lvl++) {
            final int targetLevel = lvl;
            long count = curriculum.stream().filter(l -> l.level == targetLevel).count();
            assertEquals(20, count, "Level " + lvl + " must contain exactly 20 lessons");
        }

        // Verify database persistence of 100 lessons
        List<Lesson> dbLessons = lessonDAO.findAll();
        assertNotNull(dbLessons);
        assertEquals(100, dbLessons.size(), "Database must seed all 100 curriculum lessons");

        for (int lvl = 1; lvl <= 5; lvl++) {
            List<Lesson> levelLessons = lessonDAO.findByLevel(lvl);
            assertEquals(20, levelLessons.size(), "Database must have 20 lessons for level " + lvl);
        }
    }

    @Test
    @Order(3)
    void testTextGenerator12CategoriesAndAntiRepetition() {
        assertEquals(12, TextGenerator.CATEGORIES.length, "Must offer 12 diverse text categories");

        for (String cat : TextGenerator.CATEGORIES) {
            String passage = TextGenerator.generate(cat, "Normal", 280);
            assertNotNull(passage, "Category " + cat + " must generate non-null passage");
            assertFalse(passage.isBlank(), "Category " + cat + " must not be blank");
            assertTrue(passage.length() >= 250, "Category " + cat + " must satisfy minimum length");
        }

        // Test difficulty variations
        String easyText = TextGenerator.generate("Random Words", "Easy", 300);
        String hardText = TextGenerator.generate("Random Words", "Hard", 300);
        assertNotNull(easyText);
        assertNotNull(hardText);

        // Anti-repetition test
        String t1 = TextGenerator.generate("Paragraphs", "Normal", 300);
        String t2 = TextGenerator.generate("Paragraphs", "Normal", 300);
        assertNotEquals(t1, t2, "Anti-repetition ring buffer must prevent immediate identical passage duplicates");
    }

    @Test
    @Order(4)
    void testGameSessionAndPersonalBestDAO() {
        GameSession session = new GameSession(testUser.id, "word_rush", 1500, 3, 25, 96.5, 62.0, 45);
        boolean isNewPb = gameDAO.recordSession(session);
        assertTrue(session.id > 0, "Game session ID should be populated upon insert");
        assertTrue(isNewPb, "First recorded game score must trigger a new personal best");

        // Verify PB retrieval
        GamePersonalBest pb = gameDAO.getPersonalBest(testUser.id, "word_rush");
        assertNotNull(pb);
        assertEquals(1500, pb.highScore);
        assertEquals(3, pb.highestLevel);

        // Record a lower score -> not new PB
        GameSession lowerSession = new GameSession(testUser.id, "word_rush", 800, 2, 15, 92.0, 50.0, 30);
        boolean lowerIsPb = gameDAO.recordSession(lowerSession);
        assertFalse(lowerIsPb, "Lower score must not overwrite personal best");

        // Record a higher score -> new PB
        GameSession higherSession = new GameSession(testUser.id, "word_rush", 2400, 4, 38, 98.0, 75.0, 60);
        boolean higherIsPb = gameDAO.recordSession(higherSession);
        assertTrue(higherIsPb, "Higher score must update personal best");

        GamePersonalBest updatedPb = gameDAO.getPersonalBest(testUser.id, "word_rush");
        assertEquals(2400, updatedPb.highScore);

        // Test Gamification XP award for game
        GamificationService.SessionReward reward = gamification.recordGameSession(testUser.id, higherSession, higherIsPb);
        assertNotNull(reward);
        assertTrue(reward.xpGained >= 50, "Game session with personal best and high accuracy must award XP");
        assertTrue(higherSession.xpEarned > 0);
    }

    @Test
    @Order(5)
    void testCustomTestBuilderPersistence() {
        CustomExercise ce = new CustomExercise();
        ce.userId = testUser.id;
        ce.title = "Unit Test Coding Sprint";
        ce.text = "assertNotNull(actual); assertEquals(expected, actual);";
        ce.difficulty = "Expert";

        CustomExercise saved = customDAO.insert(ce);
        assertNotNull(saved);
        assertTrue(saved.id > 0);

        List<CustomExercise> list = customDAO.findByUser(testUser.id);
        assertTrue(list.stream().anyMatch(e -> e.id == saved.id));

        // Update
        saved.title = "Updated Coding Sprint";
        customDAO.update(saved);

        List<CustomExercise> updatedList = customDAO.findByUser(testUser.id);
        CustomExercise found = updatedList.stream().filter(e -> e.id == saved.id).findFirst().orElse(null);
        assertNotNull(found);
        assertEquals("Updated Coding Sprint", found.title);

        // Delete
        customDAO.delete(saved.id);
        List<CustomExercise> afterDelete = customDAO.findByUser(testUser.id);
        assertFalse(afterDelete.stream().anyMatch(e -> e.id == saved.id));
    }
}
