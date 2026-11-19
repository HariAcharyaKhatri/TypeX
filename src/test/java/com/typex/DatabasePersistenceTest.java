package com.typex;

import com.typex.dao.*;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.model.*;
import com.typex.service.LessonService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabasePersistenceTest {

    @BeforeAll
    static void setup() throws Exception {
        DatabaseManager.getInstance().initialize();
        DatabaseInitializer.initialize();
    }

    @Test
    @Order(1)
    void connectionLifecycleProxyDoesNotCloseSingleton() throws Exception {
        Connection c1 = DatabaseManager.getInstance().getConnection();
        assertFalse(c1.isClosed());
        c1.close();
        Connection c2 = DatabaseManager.getInstance().getConnection();
        assertFalse(c2.isClosed());
        try (var st = c2.createStatement(); var rs = st.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @Order(2)
    void schemaMigrationsAppliedSuccessfully() throws Exception {
        Connection c = DatabaseManager.getInstance().getConnection();
        try (var st = c.createStatement(); var rs = st.executeQuery("PRAGMA table_info(typing_test_results)")) {
            boolean hasRawAcc = false;
            boolean hasCorrected = false;
            boolean hasUncorrected = false;
            boolean hasConsistency = false;
            boolean hasRawKeystrokes = false;

            while (rs.next()) {
                String name = rs.getString("name");
                if ("raw_accuracy".equalsIgnoreCase(name)) hasRawAcc = true;
                if ("corrected_errors".equalsIgnoreCase(name)) hasCorrected = true;
                if ("uncorrected_errors".equalsIgnoreCase(name)) hasUncorrected = true;
                if ("consistency".equalsIgnoreCase(name)) hasConsistency = true;
                if ("raw_keystrokes".equalsIgnoreCase(name)) hasRawKeystrokes = true;
            }
            assertTrue(hasRawAcc, "Migration should add raw_accuracy");
            assertTrue(hasCorrected, "Migration should add corrected_errors");
            assertTrue(hasUncorrected, "Migration should add uncorrected_errors");
            assertTrue(hasConsistency, "Migration should add consistency");
            assertTrue(hasRawKeystrokes, "Migration should add raw_keystrokes");
        }
    }

    @Test
    @Order(3)
    void userCreationAndProfileSwitching() {
        UserDAO dao = new UserDAO();
        String testUsername = "TestUser_" + System.currentTimeMillis();

        User created = dao.create(testUsername);
        assertNotNull(created);
        assertEquals(testUsername, created.username);
        assertTrue(created.id > 0);

        created.skillLevel = "Advanced";
        created.dailyGoal = 45;
        dao.update(created);

        User fetched = dao.findById(created.id);
        assertNotNull(fetched);
        assertEquals("Advanced", fetched.skillLevel);
        assertEquals(45, fetched.dailyGoal);

        dao.delete(created.id);
        assertNull(dao.findById(created.id));
    }

    @Test
    @Order(4)
    void typingResultPersistenceAndStats() {
        UserDAO userDao = new UserDAO();
        TypingResultDAO resultDao = new TypingResultDAO();
        StatisticsDAO statsDao = new StatisticsDAO();

        User u = userDao.create("StatsTester_" + System.currentTimeMillis());

        TypingResult r = new TypingResult();
        r.userId = u.id;
        r.category = "Sentences";
        r.durationSeconds = 60;
        r.grossWpm = 55.0;
        r.netWpm = 50.0;
        r.accuracy = 98.0;
        r.rawAccuracy = 96.0;
        r.correctChars = 245;
        r.incorrectChars = 5;
        r.correctedErrors = 3;
        r.uncorrectedErrors = 2;
        r.totalChars = 250;
        r.totalKeystrokes = 255;
        r.rawKeystrokes = 260;
        r.backspaces = 5;
        r.errorPercent = 2.0;
        r.consistency = 88.5;

        int resId = resultDao.insert(r);
        assertTrue(resId > 0);

        TypingResult best = resultDao.findBestByUser(u.id);
        assertNotNull(best);
        assertEquals(50.0, best.netWpm, 0.01);
        assertEquals(88.5, best.consistency, 0.01);
        assertEquals(260, best.rawKeystrokes);

        statsDao.recordTest(u.id, r.netWpm, r.accuracy, r.durationSeconds, 50);
        assertEquals(1, statsDao.currentStreak(u.id));
        assertEquals(60, statsDao.totalPracticeSeconds(u.id));

        userDao.delete(u.id);
    }

    @Test
    @Order(5)
    void characterErrorTrackingAndWeakKeys() {
        UserDAO userDao = new UserDAO();
        CharacterErrorDAO charDao = new CharacterErrorDAO();

        User u = userDao.create("CharErrorTester_" + System.currentTimeMillis());

        charDao.recordErrors(u.id, Map.of('e', 4, 't', 2, 'z', 10));

        List<CharacterError> top = charDao.getTopErrors(u.id, 5);
        assertFalse(top.isEmpty());
        assertEquals('z', (char) top.get(0).charCode, "Key with 10 mistakes should rank first");
        assertEquals(10, top.get(0).errorCount);

        Map<Character, Integer> map = charDao.getErrorCountMap(u.id);
        assertEquals(10, map.get('z'));
        assertEquals(4, map.get('e'));
        assertEquals(2, map.get('t'));

        userDao.delete(u.id);
    }

    @Test
    @Order(6)
    void lessonProgressionAndUnlockingRules() {
        UserDAO userDao = new UserDAO();
        LessonDAO lessonDao = new LessonDAO();
        LessonService lessonService = new LessonService();

        User u = userDao.create("LessonProgTester_" + System.currentTimeMillis());
        List<Lesson> lessons = lessonDao.findAll();
        assertFalse(lessons.isEmpty());

        Lesson l1 = lessons.get(0);
        Lesson l2 = lessons.get(1);

        // Lesson 1 is always unlocked
        assertTrue(lessonService.isLessonUnlocked(u.id, l1, lessons));
        // Lesson 2 is initially locked
        assertFalse(lessonService.isLessonUnlocked(u.id, l2, lessons));

        // Sub-par attempt (fails min_wpm or min_accuracy)
        lessonService.recordAttempt(u.id, l1, 5.0, 50.0, 60);
        assertFalse(lessonService.isCompleted(u.id, l1.id), "Sub-par attempt must not mark lesson completed");
        assertFalse(lessonService.isLessonUnlocked(u.id, l2, lessons), "Lesson 2 must remain locked");

        // Passing attempt
        lessonService.recordAttempt(u.id, l1, Math.max(20.0, l1.minWpm + 5), Math.max(95.0, l1.minAccuracy + 2), 45);
        assertTrue(lessonService.isCompleted(u.id, l1.id), "Passing attempt must mark lesson completed");
        assertTrue(lessonService.isLessonUnlocked(u.id, l2, lessons), "Lesson 2 must now be unlocked!");

        // Verify attempt history was recorded
        List<LessonAttempt> history = lessonService.getAttemptHistory(u.id, l1.id);
        assertEquals(2, history.size());

        userDao.delete(u.id);
    }

    @Test
    @Order(7)
    void personalBestTracking() {
        UserDAO userDao = new UserDAO();
        PersonalBestDAO pbDao = new PersonalBestDAO();

        User u = userDao.create("PbTester_" + System.currentTimeMillis());

        // First attempt sets PB
        boolean isPb1 = pbDao.recordCandidate(u.id, "Sentences", 60, 45.0, 95.0);
        assertTrue(isPb1);

        PersonalBest pb = pbDao.getBest(u.id, "Sentences", 60);
        assertNotNull(pb);
        assertEquals(45.0, pb.bestWpm, 0.01);

        // Worse attempt does not overwrite PB
        boolean isPb2 = pbDao.recordCandidate(u.id, "Sentences", 60, 40.0, 90.0);
        assertFalse(isPb2);
        assertEquals(45.0, pbDao.getBest(u.id, "Sentences", 60).bestWpm, 0.01);

        // Better attempt beats PB
        boolean isPb3 = pbDao.recordCandidate(u.id, "Sentences", 60, 52.0, 98.0);
        assertTrue(isPb3);
        assertEquals(52.0, pbDao.getBest(u.id, "Sentences", 60).bestWpm, 0.01);

        userDao.delete(u.id);
    }

    @Test
    @Order(8)
    void phase3GamificationTablesPersistence() {
        UserDAO userDao = new UserDAO();
        UserXPDAO xpDao = new UserXPDAO();
        PracticeStreakDAO streakDao = new PracticeStreakDAO();
        DailyGoalDAO goalDao = new DailyGoalDAO();
        UserPreferenceDAO prefDao = new UserPreferenceDAO();

        User u = userDao.create("GamifyDbTester_" + System.currentTimeMillis());

        // 1. XP
        UserXP xp = xpDao.getForUser(u.id);
        assertEquals(0, xp.totalXp);
        assertEquals(1, xp.level);

        UserXPDAO.XPResult res = xpDao.addXP(u.id, 150);
        assertEquals(150, res.newXp);
        assertEquals(2, res.newLevel);

        // 2. Practice Streak
        PracticeStreak streak = streakDao.getForUser(u.id);
        assertEquals(0, streak.currentStreak);

        PracticeStreakDAO.StreakUpdateResult sRes = streakDao.recordQualifyingActivity(u.id);
        assertEquals(1, sRes.currentStreak);

        // 3. Daily Goal
        UserPreference pref = prefDao.getForUser(u.id);
        DailyGoal dg = goalDao.getOrCreateToday(u.id, pref);
        assertNotNull(dg);
        assertEquals(pref.dailyGoalMinutes, dg.targetMinutes);

        // 4. Preferences
        pref.dailyGoalMinutes = 40;
        prefDao.save(pref);
        assertEquals(40, prefDao.getForUser(u.id).dailyGoalMinutes);

        userDao.delete(u.id);
    }
}
