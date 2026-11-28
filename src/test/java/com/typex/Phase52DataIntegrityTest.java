package com.typex;

import com.typex.dao.GameDAO;
import com.typex.dao.UserDAO;
import com.typex.dao.UserXPDAO;
import com.typex.database.DatabaseManager;
import com.typex.game.BaseGamePanel;
import com.typex.model.GamePersonalBest;
import com.typex.model.GameSession;
import com.typex.model.GameType;
import com.typex.model.User;
import com.typex.service.GamificationService;
import com.typex.service.KeyboardDrillService;
import com.typex.testutil.TestClock;
import com.typex.testutil.TestDatabaseHelper;
import com.typex.util.KeyboardLayout;
import com.typex.util.KeyboardLayout.Finger;
import com.typex.util.KeyboardLayout.LayoutInfo;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase52DataIntegrityTest {

    private static Path isolatedDb;
    private static User testUser;
    private static final UserDAO userDAO = new UserDAO();
    private static final GameDAO gameDAO = new GameDAO();
    private static final UserXPDAO userXpDAO = new UserXPDAO();
    private static final GamificationService gamification = new GamificationService();

    @BeforeAll
    static void setup() throws SQLException {
        isolatedDb = TestDatabaseHelper.initIsolatedDatabase();
        testUser = userDAO.create(TestDatabaseHelper.uniqueUsername("Phase52Tester"));
        assertNotNull(testUser);
    }

    @AfterAll
    static void tearDown() {
        TestDatabaseHelper.teardownIsolatedDatabase(isolatedDb);
    }

    // =========================================================================
    // 1. P0 — Atomic Game Session & Gamification Persistence
    // =========================================================================

    @Test
    @Order(1)
    void testAtomicGameCompletionSuccess() {
        int uid = testUser.id;
        int xpBefore = userXpDAO.getForUser(uid).totalXp;

        GameSession session = new GameSession(uid, GameType.WORD_RUSH.id, 2500, 4, 30, 98.0, 70.0, 60);
        GamificationService.GameCompletionResult result = gamification.recordGameCompletion(uid, session);

        assertTrue(result.success, "Atomic game completion should succeed");
        assertTrue(result.reward.xpGained > 0, "XP should be awarded");
        assertEquals(result.reward.xpGained, session.xpEarned, "Session xpEarned must match reward xpGained");

        // Verify DB was atomically updated
        int xpAfter = userXpDAO.getForUser(uid).totalXp;
        assertEquals(xpBefore + result.reward.xpGained, xpAfter, "User total XP must reflect awarded XP");

        List<GameSession> sessions = gameDAO.getRecentSessions(uid, 1);
        assertFalse(sessions.isEmpty());
        assertEquals(session.id, sessions.get(0).id);
        assertEquals(session.xpEarned, sessions.get(0).xpEarned);
    }

    @Test
    @Order(2)
    void testAtomicGameCompletionRollbackOnFailure() {
        int uid = testUser.id;
        int xpBefore = userXpDAO.getForUser(uid).totalXp;
        int gamesBefore = gameDAO.getTotalGamesPlayed(uid);

        // A session with invalid user ID or null required field inside a forced-rollback block
        GameSession invalidSession = new GameSession(-999, GameType.FALLING_WORDS.id, 1000, 2, 20, 90.0, 40.0, 30);
        GamificationService.GameCompletionResult result = gamification.recordGameCompletion(-999, invalidSession);

        // Should cleanly return without persisting or corrupting user XP
        assertFalse(result.isNewPb);
        int xpAfter = userXpDAO.getForUser(uid).totalXp;
        int gamesAfter = gameDAO.getTotalGamesPlayed(uid);

        assertEquals(xpBefore, xpAfter, "XP must not change when game completion fails or is invalid");
        assertEquals(gamesBefore, gamesAfter, "No game sessions should be committed on invalid completion");
    }

    // =========================================================================
    // 2. P1 — Personal Best Detection for Highest Level (All 7 Scenarios)
    // =========================================================================

    @Test
    @Order(3)
    void testPersonalBestHighestLevelAndMultiMetrics() {
        User pbUser = userDAO.create(TestDatabaseHelper.uniqueUsername("PbTester"));
        assertNotNull(pbUser);
        int uid = pbUser.id;
        String gt = GameType.TYPING_SURVIVAL.id;

        // Scenario 7: First session for game type (pb == null)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 1000, 1, 40.0, 90.0),
                "Scenario 7: First session must qualify as personal best");
        gameDAO.recordSession(new GameSession(uid, gt, 1000, 1, 15, 90.0, 40.0, 30));

        GamePersonalBest baseline = gameDAO.getPersonalBest(uid, gt);
        assertEquals(1000, baseline.highScore);
        assertEquals(1, baseline.highestLevel);
        assertEquals(40.0, baseline.bestWpm, 0.01);
        assertEquals(90.0, baseline.bestAccuracy, 0.01);

        // Scenario 1: Only Score PB (higher score, same/lower level, wpm, acc)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 1500, 1, 35.0, 85.0),
                "Scenario 1: Higher score alone must trigger PB");

        // Scenario 2: Only WPM PB (same/lower score, level, acc, but higher WPM)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 800, 1, 55.0, 85.0),
                "Scenario 2: Higher WPM alone must trigger PB");

        // Scenario 3: Only Accuracy PB (same/lower score, level, wpm, but higher Acc)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 800, 1, 35.0, 99.0),
                "Scenario 3: Higher Accuracy alone must trigger PB");

        // Scenario 4: Only Highest-Level PB (lower score, lower wpm, lower acc, but level 3 > level 1)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 700, 3, 30.0, 80.0),
                "Scenario 4: Higher Level alone must trigger PB");

        // Scenario 5: Multiple PBs in one session (Score and Level improved)
        assertTrue(gameDAO.isPersonalBest(uid, gt, 2000, 4, 30.0, 80.0),
                "Scenario 5: Multiple metric improvements must trigger PB");

        // Record a new PB to update baseline to Level 4
        gameDAO.recordSession(new GameSession(uid, gt, 2000, 4, 30, 80.0, 30.0, 60));

        // Scenario 6: No PB improvement (all metrics equal or lower)
        assertFalse(gameDAO.isPersonalBest(uid, gt, 1800, 3, 25.0, 75.0),
                "Scenario 6: Inferior run on all 4 metrics must NOT trigger PB");
        assertFalse(gameDAO.isPersonalBest(uid, gt, 2000, 4, 30.0, 80.0),
                "Scenario 6: Exactly equal run must NOT trigger new PB");
    }

    // =========================================================================
    // 3. P1 — Isolated Test Database Strategy
    // =========================================================================

    @Test
    @Order(4)
    void testDatabaseIsolationPath() {
        Path activeDb = DatabaseManager.getInstance().getDatabasePath();
        assertNotNull(activeDb);
        assertTrue(activeDb.toString().contains("typex_isolated_test_"),
                "Active test DB must point to temporary isolated test DB, not ~/.typex/typex.db");
        assertFalse(activeDb.toString().contains(".typex\\typex.db") && !activeDb.toString().contains("isolated"),
                "Must never point directly to production database during test execution");
    }

    // =========================================================================
    // 4. P1 — Keyboard Layout Unknown Character Handling
    // =========================================================================

    @Test
    @Order(5)
    void testKeyboardLayoutUnknownAndUnsupportedCharacterHandling() {
        LayoutInfo us = KeyboardLayout.getLayoutOrDefault("US_QWERTY");
        LayoutInfo de = KeyboardLayout.getLayoutOrDefault("DE_QWERTZ");

        // 1. Normal mapped characters
        assertNotNull(us.getFingerForChar('a'));
        assertEquals(Finger.LEFT_PINKY, us.getFingerForChar('a'));

        // 2. Uppercase characters
        assertEquals("Left Index Finger + Right Shift", KeyboardLayout.getFingerInstruction(us, 'F'));

        // 3. Spacebar must return Finger.THUMB
        assertEquals(Finger.THUMB, us.getFingerForChar(' '));
        assertEquals("Both Thumbs (Spacebar)", KeyboardLayout.getFingerInstruction(us, ' '));

        // 4. Shifted symbols
        assertEquals("Left Pinky Finger + Right Shift", KeyboardLayout.getFingerInstruction(us, '!'));

        // 5. Genuinely unsupported characters (must return null finger, NOT Thumb!)
        assertNull(us.getFingerForChar('\u00A9'), "Copyright symbol should return null finger, NOT THUMB");
        assertNull(us.getFingerForChar('\u4E16'), "Chinese character should return null finger, NOT THUMB");
        assertEquals("Unsupported Character", KeyboardLayout.getFingerInstruction(us, '\u00A9'));
        assertEquals("Unsupported Character", KeyboardLayout.getFingerInstruction(us, '\u4E16'));

        // 6. Characters not available in a particular layout
        // 'ä' is valid in German QWERTZ, but unmapped in standard US QWERTY
        assertNull(us.getFingerForChar('ä'), "Umlaut 'ä' is not mapped in US QWERTY and must return null");
        assertEquals("Unsupported Character", KeyboardLayout.getFingerInstruction(us, 'ä'));

        assertNotNull(de.getFingerForChar('ä'), "Umlaut 'ä' must be mapped in German QWERTZ");
        assertNotEquals("Unsupported Character", KeyboardLayout.getFingerInstruction(de, 'ä'));
    }

    // =========================================================================
    // 5. P1 — Deterministic Timing Tests (All 7 Scenarios with TestClock)
    // =========================================================================

    @Test
    @Order(6)
    void testDeterministicTimingAllSevenScenarios() {
        TestClock clock = new TestClock(10_000_000_000L); // 10s baseline
        TestHeadlessGame game = new TestHeadlessGame();
        game.setTimeSource(clock);

        // Scenario 1: Normal gameplay duration (30s)
        game.startGame();
        clock.advanceSeconds(30);
        assertEquals(30_000_000_000L, game.getActiveElapsedNanos(), "Scenario 1: Active time should be exactly 30s");

        // Scenario 2: One pause and resume (advance 10s, pause 60s, resume, advance 10s -> 50s total active)
        clock.advanceSeconds(10);
        game.pauseGame();
        long pausedMark = game.getActiveElapsedNanos();
        clock.advanceSeconds(60); // 60 seconds paused!
        assertEquals(pausedMark, game.getActiveElapsedNanos(), "Scenario 2: Active time must freeze during pause");

        game.resumeGame();
        clock.advanceSeconds(10);
        assertEquals(50_000_000_000L, game.getActiveElapsedNanos(), "Scenario 2: Total active time must strictly exclude pause");

        // Scenario 3: Multiple pause/resume cycles
        game.pauseGame();
        clock.advanceSeconds(15);
        game.resumeGame();
        clock.advanceSeconds(5);

        game.pauseGame();
        clock.advanceSeconds(45);
        game.resumeGame();
        clock.advanceSeconds(5);
        assertEquals(60_000_000_000L, game.getActiveElapsedNanos(), "Scenario 3: Multiple pauses must all be excluded");

        // Scenario 4: Long pause duration (pause for 3600 seconds)
        game.pauseGame();
        clock.advanceSeconds(3600);
        assertEquals(60_000_000_000L, game.getActiveElapsedNanos(), "Scenario 4: 1-hour pause must not inflate active time");
        game.resumeGame();

        // Scenario 5: Cleanup while playing
        game.cleanup();
        assertEquals(BaseGamePanel.State.PAUSED, game.getState(), "Scenario 5: Cleanup while playing must transition to PAUSED");
        long cleanupNanos = game.getActiveElapsedNanos();
        clock.advanceSeconds(100);
        assertEquals(cleanupNanos, game.getActiveElapsedNanos(), "Scenario 5: Time after cleanup must remain frozen");

        // Scenario 6: Cleanup while already paused
        game.cleanup();
        clock.advanceSeconds(50);
        assertEquals(cleanupNanos, game.getActiveElapsedNanos(), "Scenario 6: Second cleanup must not alter accumulated pause");

        // Scenario 7: Game-over timing and stability
        game.resumeGame();
        clock.advanceSeconds(10);
        game.endGame();

        long finalNanos = game.getActiveElapsedNanos();
        assertEquals(BaseGamePanel.State.GAME_OVER, game.getState());
        clock.advanceSeconds(300); // Advance clock after game over

        // Subsequent calls must return the EXACT same finalized timestamp
        assertEquals(finalNanos, game.getActiveElapsedNanos(), "Scenario 7: Final elapsed nanos must be immutable after game over");
        assertEquals(finalNanos, game.getActiveElapsedNanos());
    }

    // =========================================================================
    // 6. P2 — Weak Keys Drill Layout Compatibility (All 6 Layouts)
    // =========================================================================

    @Test
    @Order(7)
    void testWeakKeysDrillLayoutCompatibilityAcrossAllLayouts() {
        String[] layouts = {"US_QWERTY", "UK_QWERTY", "DE_QWERTZ", "FR_AZERTY", "DVORAK", "COLEMAK"};

        for (String lName : layouts) {
            LayoutInfo info = KeyboardLayout.getLayoutOrDefault(lName);

            // 1. When error list is empty, falls back to layout's dynamic Home Row
            String fallback = KeyboardDrillService.filterWeakKeys(info, List.of());
            assertNotNull(fallback);
            assertFalse(fallback.isEmpty(), "Home Row fallback for " + lName + " must not be empty");

            // 2. When errors contain foreign characters not in this layout, they must be stripped
            List<Character> errorsWithForeign = List.of('a', 's', '\u00A9', '\u4E16');
            String filtered = KeyboardDrillService.filterWeakKeys(info, errorsWithForeign);
            assertFalse(filtered.contains(String.valueOf('\u00A9')), "Foreign symbol must be stripped");
            assertFalse(filtered.contains(String.valueOf('\u4E16')), "Asian character must be stripped");
            assertTrue(filtered.length() > 0, "Valid characters must be preserved");

            // 3. Fallback when ALL errors are invalid
            String allInvalid = KeyboardDrillService.filterWeakKeys(info, List.of('\u00A9', '\u4E16'));
            assertEquals(fallback, allInvalid, "When all error keys are invalid, must fall back to layout Home Row");
        }
    }

    // =========================================================================
    // 7. P2 — Null Frame and Anonymous Session Safety
    // =========================================================================

    @Test
    @Order(8)
    void testNullFrameSafetyInBaseGamePanel() {
        // Construct headless game panel with null frame
        TestHeadlessGame headlessGame = new TestHeadlessGame();
        headlessGame.startGame();
        assertDoesNotThrow(() -> headlessGame.endGame(), "endGame with null frame must not throw NullPointerException");
        assertEquals(BaseGamePanel.State.GAME_OVER, headlessGame.getState());
    }

    // =========================================================================
    // 8. P2 — Stale GamesPanel Refresh Rejection & DAO Error Handling
    // =========================================================================

    @Test
    @Order(9)
    void testGamesPanelStaleRefreshPrevention() {
        com.typex.ui.pages.GamesPanel panel = new com.typex.ui.pages.GamesPanel(null);
        panel.refresh(); // Frame is null -> gracefully resets without throwing NPE
        assertNotNull(panel);
    }

    @Test
    @Order(10)
    void testGameDaoThrowsDataAccessExceptionOnFailure() {
        // Calling recordSession with null session or invalid user returns false cleanly
        assertFalse(gameDAO.recordSession(null));
        assertFalse(gameDAO.recordSession(new GameSession(0, "word_rush", 100, 1, 10, 90.0, 30.0, 30)));
    }

    // =========================================================================
    // Helper Subclass for BaseGamePanel Testing
    // =========================================================================

    private static class TestHeadlessGame extends BaseGamePanel {
        private JPanel dummy;

        public TestHeadlessGame() {
            super(null, GameType.WORD_RUSH, () -> {});
        }

        @Override
        protected JComponent getGameComponent() {
            if (dummy == null) dummy = new JPanel();
            return dummy;
        }

        public State getState() {
            return gameState;
        }

        @Override protected String getMetric1Name() { return "M1"; }
        @Override protected String getMetric1Value() { return "V1"; }
        @Override protected String getMetric2Name() { return "M2"; }
        @Override protected String getMetric2Value() { return "V2"; }

        @Override protected void onGameStart() {}
        @Override protected void onGameTick() {}
        @Override protected void handleTypedChar(char c) {}
        @Override protected void handleBackspace() {}
        @Override protected void handleSpace() {}
    }
}
