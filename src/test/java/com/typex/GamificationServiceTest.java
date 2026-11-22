package com.typex;

import com.typex.dao.*;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.model.*;
import com.typex.service.GamificationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GamificationServiceTest {
    private static java.nio.file.Path isolatedDb;
    private static UserDAO userDAO;
    private static GamificationService gamificationService;
    private static User testUser;

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("java.awt.headless", "true");
        isolatedDb = com.typex.testutil.TestDatabaseHelper.initIsolatedDatabase();
        userDAO = new UserDAO();
        gamificationService = new GamificationService();
    }

    @AfterAll
    public static void tearDown() {
        com.typex.testutil.TestDatabaseHelper.teardownIsolatedDatabase(isolatedDb);
    }

    @BeforeEach
    public void createTestUser() {
        String uname = "gamify_test_" + System.currentTimeMillis();
        testUser = userDAO.create(uname);
        assertNotNull(testUser);
    }

    @Test
    public void testLevelProgressionMath() {
        // Level math: Level = 1 + floor(sqrt(xp / 100))
        assertEquals(1, UserXPDAO.calculateLevel(0));
        assertEquals(1, UserXPDAO.calculateLevel(50));
        assertEquals(1, UserXPDAO.calculateLevel(99));
        assertEquals(2, UserXPDAO.calculateLevel(100));
        assertEquals(2, UserXPDAO.calculateLevel(250));
        assertEquals(2, UserXPDAO.calculateLevel(399));
        assertEquals(3, UserXPDAO.calculateLevel(400));
        assertEquals(4, UserXPDAO.calculateLevel(900));

        assertEquals(0, UserXPDAO.xpThresholdForLevel(1));
        assertEquals(100, UserXPDAO.xpThresholdForLevel(2));
        assertEquals(400, UserXPDAO.xpThresholdForLevel(3));

        assertEquals(100, UserXPDAO.xpThresholdForNextLevel(1));
        assertEquals(400, UserXPDAO.xpThresholdForNextLevel(2));
    }

    @Test
    public void testXpAndLevelUpAward() {
        UserXP initial = gamificationService.getUserXP(testUser.id);
        assertEquals(0, initial.totalXp);
        assertEquals(1, initial.level);

        TypingResult r = new TypingResult();
        r.userId = testUser.id;
        r.durationSeconds = 60;
        r.netWpm = 45.0;
        r.accuracy = 96.0; // >= 95% awards +20 XP bonus
        r.totalKeystrokes = 150;

        GamificationService.SessionReward reward = gamificationService.recordTestCompletion(testUser.id, r, false);
        // Base 50 + 20 accuracy bonus = 70 XP (+ achievements and challenges)
        assertTrue(reward.xpGained >= 70, "Earned XP should be at least 70");
        assertTrue(reward.totalXp >= 70);
        assertTrue(reward.newLevel >= 1);

        // Add more XP to verify level progression
        UserXPDAO xpDao = new UserXPDAO();
        int xpBefore = reward.totalXp;
        UserXPDAO.XPResult res = xpDao.addXP(testUser.id, 500);
        assertEquals(xpBefore + 500, res.newXp);
        assertTrue(res.newLevel >= reward.newLevel);
        assertTrue(res.leveledUp || res.newLevel > reward.oldLevel);
    }

    @Test
    public void testPersonalBestXpBonus() {
        TypingResult r = new TypingResult();
        r.userId = testUser.id;
        r.durationSeconds = 60;
        r.netWpm = 65.0;
        r.accuracy = 99.0; // >= 98% gives +40 XP
        r.totalKeystrokes = 200;

        // With isNewPb = true, +100 XP bonus is awarded
        GamificationService.SessionReward reward = gamificationService.recordTestCompletion(testUser.id, r, true);
        // Base 50 + 40 accuracy + 100 PB = 190 XP
        assertTrue(reward.xpGained >= 190, "Expected at least 190 XP with PB bonus");
    }

    @Test
    public void testPracticeStreakContinuationAndAntiDuplicate() {
        PracticeStreakDAO streakDao = new PracticeStreakDAO();
        PracticeStreakDAO.StreakUpdateResult res1 = streakDao.recordQualifyingActivity(testUser.id);
        assertEquals(1, res1.currentStreak);
        assertFalse(res1.alreadyCountedToday);

        // Second qualifying activity today should not increment streak again
        PracticeStreakDAO.StreakUpdateResult res2 = streakDao.recordQualifyingActivity(testUser.id);
        assertEquals(1, res2.currentStreak);
        assertTrue(res2.alreadyCountedToday);

        PracticeStreak s = streakDao.getForUser(testUser.id);
        assertEquals(1, s.currentStreak);
    }

    @Test
    public void testDailyGoalProgressAndCompletion() {
        DailyGoalDAO goalDao = new DailyGoalDAO();
        UserPreferenceDAO prefDao = new UserPreferenceDAO();

        UserPreference pref = prefDao.getForUser(testUser.id);
        pref.dailyGoalMinutes = 2; // small target for testing
        pref.dailyGoalTests = 2;
        pref.dailyGoalLessons = 0; // 0 lessons needed
        prefDao.save(pref);

        DailyGoal goal = goalDao.getOrCreateToday(testUser.id, pref);
        assertEquals(2, goal.targetMinutes);
        assertEquals(2, goal.targetTests);
        assertEquals(0, goal.completedTests);
        assertFalse(goal.achieved);

        // Test 1: 60 seconds
        boolean achieved1 = goalDao.recordTest(testUser.id, 60, 95.0);
        assertFalse(achieved1);

        // Test 2: 60 seconds (now 120s = 2 min, 2 tests completed)
        boolean achieved2 = goalDao.recordTest(testUser.id, 60, 96.0);
        assertTrue(achieved2, "Daily goal should be achieved after meeting all targets");

        // Verify claim reward
        boolean claimed1 = goalDao.claimReward(testUser.id, goal.date);
        assertTrue(claimed1, "Reward should be claimable once");

        // Double claim prevention
        boolean claimed2 = goalDao.claimReward(testUser.id, goal.date);
        assertFalse(claimed2, "Duplicate reward claim should be prevented");
    }

    @Test
    public void testDeterministicChallenges() {
        List<UserChallenge> list = gamificationService.getActiveChallenges(testUser.id);
        assertNotNull(list);
        assertFalse(list.isEmpty());

        long dailyCount = list.stream().filter(uc -> "DAILY".equalsIgnoreCase(uc.challenge.type)).count();
        long weeklyCount = list.stream().filter(uc -> "WEEKLY".equalsIgnoreCase(uc.challenge.type)).count();
        assertEquals(3, dailyCount, "Should generate exactly 3 daily challenges");
        assertEquals(2, weeklyCount, "Should generate exactly 2 weekly challenges");
    }

    @Test
    public void testUserPreferencesPersistence() {
        UserPreferenceDAO prefDao = new UserPreferenceDAO();
        UserPreference p = prefDao.getForUser(testUser.id);
        p.dailyGoalMinutes = 25;
        p.dailyGoalTests = 5;
        p.dailyGoalLessons = 2;
        p.dailyGoalAccuracy = 97.5;
        p.preferredDuration = 120;
        p.preferredCategory = "Programming";
        p.soundEnabled = false;
        p.volume = 40;
        prefDao.save(p);

        UserPreference loaded = prefDao.getForUser(testUser.id);
        assertEquals(25, loaded.dailyGoalMinutes);
        assertEquals(5, loaded.dailyGoalTests);
        assertEquals(2, loaded.dailyGoalLessons);
        assertEquals(97.5, loaded.dailyGoalAccuracy, 0.01);
        assertEquals(120, loaded.preferredDuration);
        assertEquals("Programming", loaded.preferredCategory);
        assertFalse(loaded.soundEnabled);
        assertEquals(40, loaded.volume);
    }
}
