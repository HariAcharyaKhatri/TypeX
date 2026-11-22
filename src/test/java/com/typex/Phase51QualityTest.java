package com.typex;

import com.typex.dao.GameDAO;
import com.typex.dao.UserDAO;
import com.typex.database.DatabaseInitializer;
import com.typex.game.BaseGamePanel;
import com.typex.model.GamePersonalBest;
import com.typex.model.GameSession;
import com.typex.model.GameType;
import com.typex.model.User;
import com.typex.service.KeyboardDrillService;
import com.typex.ui.MainFrame;
import com.typex.util.KeyboardLayout;
import com.typex.util.KeyboardLayout.LayoutInfo;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase51QualityTest {

    private static User testUser;
    private static final UserDAO userDAO = new UserDAO();
    private static final GameDAO gameDAO = new GameDAO();

    @BeforeAll
    static void setup() throws SQLException {
        DatabaseInitializer.initialize();
        String testUsername = "Phase51Tester_" + System.currentTimeMillis();
        testUser = userDAO.create(testUsername);
        assertNotNull(testUser, "Test user must be initialized");
    }

    @Test
    @Order(1)
    void testKeyboardLayoutResolutionAndFallback() {
        // 1. All 6 layout IDs and display names resolve correctly
        LayoutInfo us = KeyboardLayout.getLayoutOrDefault("US_QWERTY");
        assertEquals("US_QWERTY", us.id);
        assertEquals("US QWERTY", us.displayName);

        LayoutInfo usName = KeyboardLayout.getLayoutOrDefault("US QWERTY");
        assertEquals("US_QWERTY", usName.id);

        LayoutInfo uk = KeyboardLayout.getLayoutOrDefault("UK_QWERTY");
        assertEquals("UK_QWERTY", uk.id);
        LayoutInfo ukName = KeyboardLayout.getLayoutOrDefault("UK QWERTY");
        assertEquals("UK_QWERTY", ukName.id);

        LayoutInfo de = KeyboardLayout.getLayoutOrDefault("DE_QWERTZ");
        assertEquals("GERMAN_QWERTZ", de.id);
        LayoutInfo deName = KeyboardLayout.getLayoutOrDefault("German QWERTZ");
        assertEquals("GERMAN_QWERTZ", deName.id);

        LayoutInfo fr = KeyboardLayout.getLayoutOrDefault("FR_AZERTY");
        assertEquals("FRENCH_AZERTY", fr.id);
        LayoutInfo frName = KeyboardLayout.getLayoutOrDefault("French AZERTY");
        assertEquals("FRENCH_AZERTY", frName.id);

        LayoutInfo dvorak = KeyboardLayout.getLayoutOrDefault("DVORAK");
        assertEquals("DVORAK", dvorak.id);
        LayoutInfo dvorakName = KeyboardLayout.getLayoutOrDefault("Dvorak Simplified");
        assertEquals("DVORAK", dvorakName.id);

        LayoutInfo colemak = KeyboardLayout.getLayoutOrDefault("COLEMAK");
        assertEquals("COLEMAK", colemak.id);
        LayoutInfo colemakName = KeyboardLayout.getLayoutOrDefault("Colemak");
        assertEquals("COLEMAK", colemakName.id);

        // 2. Safe Fallbacks: unknown names, null, empty strings never throw and fall back to US QWERTY
        LayoutInfo fallbackUnknown = KeyboardLayout.getLayoutOrDefault("UNKNOWN_LAYOUT_123");
        assertNotNull(fallbackUnknown);
        assertEquals("US_QWERTY", fallbackUnknown.id);

        LayoutInfo fallbackNull = KeyboardLayout.getLayoutOrDefault(null);
        assertNotNull(fallbackNull);
        assertEquals("US_QWERTY", fallbackNull.id);

        LayoutInfo fallbackEmpty = KeyboardLayout.getLayoutOrDefault("   ");
        assertNotNull(fallbackEmpty);
        assertEquals("US_QWERTY", fallbackEmpty.id);

        // 3. Verify getLayout delegates to getLayoutOrDefault
        assertEquals("GERMAN_QWERTZ", KeyboardLayout.getLayout("German QWERTZ").id);
        assertEquals("FRENCH_AZERTY", KeyboardLayout.getLayout("French AZERTY").id);
    }

    @Test
    @Order(2)
    void testKeyboardDrillsLayoutAwareness() {
        LayoutInfo us = KeyboardLayout.getLayoutOrDefault("US_QWERTY");
        LayoutInfo de = KeyboardLayout.getLayoutOrDefault("DE_QWERTZ");
        LayoutInfo fr = KeyboardLayout.getLayoutOrDefault("FR_AZERTY");
        LayoutInfo dvorak = KeyboardLayout.getLayoutOrDefault("DVORAK");

        // Top Row tests
        String usTop = KeyboardDrillService.getDrillPool(us, "Top Row");
        assertTrue(usTop.contains("q") && usTop.contains("w") && usTop.contains("e") && usTop.contains("y"),
                "US Top row must contain q, w, e, y");
        assertFalse(usTop.contains("z"), "US Top row must not contain z");

        String deTop = KeyboardDrillService.getDrillPool(de, "Top Row");
        assertTrue(deTop.contains("z"), "German QWERTZ Top row must contain z");
        assertFalse(deTop.contains("y"), "German QWERTZ Top row must not contain y");

        String frTop = KeyboardDrillService.getDrillPool(fr, "Top Row");
        assertTrue(frTop.contains("a") && frTop.contains("z"), "French AZERTY Top row must contain a and z");

        // Home Row tests
        String usHome = KeyboardDrillService.getDrillPool(us, "Home Row");
        assertTrue(usHome.contains("a") && usHome.contains("s") && usHome.contains("d") && usHome.contains("f"),
                "US Home row must contain asdf");

        String dvorakHome = KeyboardDrillService.getDrillPool(dvorak, "Home Row");
        assertTrue(dvorakHome.contains("a") && dvorakHome.contains("o") && dvorakHome.contains("e") && dvorakHome.contains("u") && dvorakHome.contains("i"),
                "Dvorak Home row must contain aoeui");

        // Finger isolation tests (Index, Pinky)
        String indexPool = KeyboardDrillService.getDrillPool(us, "Index Fingers");
        assertTrue(indexPool.contains("f") && indexPool.contains("j") && indexPool.contains("r") && indexPool.contains("u"),
                "Index fingers on US must contain f, j, r, u");
        assertFalse(indexPool.contains("a") || indexPool.contains(";"),
                "Index fingers must not contain pinky keys like a or ;");

        // Control keys excluded
        String randomPool = KeyboardDrillService.getDrillPool(us, "Random Keys");
        assertFalse(randomPool.contains("SHIFT") || randomPool.contains("ENTER") || randomPool.contains("CTRL"),
                "Control/modifier keys must never be included in drill character pools");
    }

    @Test
    @Order(3)
    void testFingerGuidanceAndOppositeHandShift() {
        LayoutInfo us = KeyboardLayout.getLayoutOrDefault("US_QWERTY");

        // Unshifted keys
        String fInst = KeyboardLayout.getFingerInstruction(us, 'f');
        assertTrue(fInst.contains("Left") && fInst.contains("Index"), "F must be Left Index finger: " + fInst);

        String jInst = KeyboardLayout.getFingerInstruction(us, 'j');
        assertTrue(jInst.contains("Right") && jInst.contains("Index"), "J must be Right Index finger: " + jInst);

        // Shifted key on left hand: requires opposite (Right Hand) Shift
        String capFInst = KeyboardLayout.getFingerInstruction(us, 'F');
        assertTrue(capFInst.contains("Left") && capFInst.contains("Index"), "Capital F finger must be Left Index");
        assertTrue(capFInst.contains("Right Shift"), "Capital F on left hand must instruct Right Shift: " + capFInst);

        // Shifted key on right hand: requires opposite (Left Hand) Shift
        String capJInst = KeyboardLayout.getFingerInstruction(us, 'J');
        assertTrue(capJInst.contains("Right") && capJInst.contains("Index"), "Capital J finger must be Right Index");
        assertTrue(capJInst.contains("Left Shift"), "Capital J on right hand must instruct Left Shift: " + capJInst);

        // Symbol with shift: '!' on US layout is on key 1 (Left Pinky)
        String exclInst = KeyboardLayout.getFingerInstruction(us, '!');
        assertTrue(exclInst.contains("Left") && exclInst.contains("Pinky"), "! must be typed with Left Pinky: " + exclInst);
        assertTrue(exclInst.contains("Right Shift"), "! on left hand must instruct Right Shift: " + exclInst);
    }

    @Test
    @Order(4)
    void testGameActiveMonotonicTimeAccounting() throws InterruptedException {
        // Subclass BaseGamePanel to test pause/resume monotonic timing model
        TestGamePanel game = new TestGamePanel();
        game.startGame();

        // Simulate active play of 50ms
        Thread.sleep(50);
        long active1 = game.getActiveElapsedNanos();
        assertTrue(active1 >= 40_000_000L, "Active time should be at least 40ms: " + active1);

        // Pause game for 100ms
        game.pauseGame();
        long pausedAt = game.getActiveElapsedNanos();
        Thread.sleep(100);
        long pausedCheck = game.getActiveElapsedNanos();
        // Time while paused should be completely frozen
        assertEquals(pausedAt, pausedCheck, "Active elapsed time must not increment while paused");

        // Resume game and play for another 50ms
        game.resumeGame();
        Thread.sleep(50);
        long active2 = game.getActiveElapsedNanos();
        // Total active time should be around 100ms, strictly excluding the 100ms paused interval
        assertTrue(active2 < 200_000_000L, "Active time must strictly exclude pause duration: " + active2);
        assertTrue(active2 >= 80_000_000L, "Active time must accumulate active intervals: " + active2);

        game.cleanup();
    }

    @Test
    @Order(5)
    void testAtomicGameSessionAndXpPersistence() {
        int uid = testUser.id;
        int score = 1500;
        int level = 3;
        int words = 25;
        double acc = 96.0;
        double wpm = 52.0;
        int duration = 60;

        GameSession session = new GameSession(uid, GameType.WORD_RUSH.id, score, level, words, acc, wpm, duration);
        session.xpEarned = 125; // Pre-calculated XP

        boolean isNewPb = gameDAO.recordSession(session);
        assertTrue(isNewPb, "First session with score > 0 must be a personal best");
        assertTrue(session.id > 0, "Session ID must be generated by database");

        // Verify persisted XP in SQLite table game_sessions
        List<GameSession> recent = gameDAO.getRecentSessions(uid, 5);
        assertFalse(recent.isEmpty(), "Recent sessions must not be empty");
        GameSession recorded = recent.get(0);
        assertEquals(session.id, recorded.id);
        assertEquals(125, recorded.xpEarned, "XP earned must be accurately persisted in game_sessions table");
    }

    @Test
    @Order(6)
    void testIndependentPersonalBestMetrics() {
        int uid = testUser.id;
        String gameType = GameType.LETTER_INVADERS.id;

        // Session 1: High score 2000, 40 WPM, 85% Accuracy
        GameSession s1 = new GameSession(uid, gameType, 2000, 2, 20, 85.0, 40.0, 60);
        s1.xpEarned = 80;
        boolean pb1 = gameDAO.recordSession(s1);
        assertTrue(pb1);

        GamePersonalBest pbAfter1 = gameDAO.getPersonalBest(uid, gameType);
        assertNotNull(pbAfter1);
        assertEquals(2000, pbAfter1.highScore);
        assertEquals(40.0, pbAfter1.bestWpm, 0.01);
        assertEquals(85.0, pbAfter1.bestAccuracy, 0.01);

        // Session 2: Lower score (1500) but higher WPM (65.0) and higher Accuracy (98.0%)
        assertTrue(gameDAO.isPersonalBest(uid, gameType, 1500, 65.0, 98.0),
                "isPersonalBest must report true when WPM or accuracy beats previous best");

        GameSession s2 = new GameSession(uid, gameType, 1500, 2, 25, 98.0, 65.0, 60);
        s2.xpEarned = 110;
        boolean pb2 = gameDAO.recordSession(s2);
        assertTrue(pb2, "recordSession must register as personal best when WPM/accuracy improves");

        // Verify that high score was preserved while best WPM and accuracy were updated
        GamePersonalBest pbAfter2 = gameDAO.getPersonalBest(uid, gameType);
        assertNotNull(pbAfter2);
        assertEquals(2000, pbAfter2.highScore, "High score must not regress when subsequent run scores lower");
        assertEquals(65.0, pbAfter2.bestWpm, 0.01, "Best WPM must be independently upgraded");
        assertEquals(98.0, pbAfter2.bestAccuracy, 0.01, "Best accuracy must be independently upgraded");
    }

    @Test
    @Order(7)
    void testFavoriteGameCalculationAndTieBreaking() {
        String testFavUser = "FavTester_" + System.currentTimeMillis();
        User favUser = userDAO.create(testFavUser);
        assertNotNull(favUser);

        // 0 games played -> returns null (UI shows 'No games yet')
        assertNull(gameDAO.getFavoriteGameType(favUser.id), "With no sessions, favorite game should be null");

        // Play 2 games of CODE_RUNNER and 1 game of WORD_RUSH
        gameDAO.recordSession(new GameSession(favUser.id, GameType.CODE_RUNNER.id, 500, 1, 10, 90.0, 30.0, 30));
        gameDAO.recordSession(new GameSession(favUser.id, GameType.CODE_RUNNER.id, 600, 1, 12, 92.0, 32.0, 30));
        gameDAO.recordSession(new GameSession(favUser.id, GameType.WORD_RUSH.id, 400, 1, 8, 85.0, 25.0, 30));

        assertEquals(GameType.CODE_RUNNER.id, gameDAO.getFavoriteGameType(favUser.id),
                "Most played game type should be returned as favorite");

        // Add 1 more WORD_RUSH so both CODE_RUNNER and WORD_RUSH have 2 plays
        // Tie-breaker: ORDER BY cnt DESC, game_type ASC -> 'code_runner' comes before 'word_rush'
        gameDAO.recordSession(new GameSession(favUser.id, GameType.WORD_RUSH.id, 450, 1, 9, 88.0, 28.0, 30));
        assertEquals(GameType.CODE_RUNNER.id, gameDAO.getFavoriteGameType(favUser.id),
                "Tie-breaker must deterministically select alphabetical game_type");
    }

    // Helper subclass to verify BaseGamePanel timing and lifecycle in headless test
    private static class TestGamePanel extends BaseGamePanel {
        private JPanel dummy;

        public TestGamePanel() {
            super(null, GameType.WORD_RUSH, () -> {});
        }

        @Override
        protected JComponent getGameComponent() {
            if (dummy == null) {
                dummy = new JPanel();
            }
            return dummy;
        }

        @Override
        protected String getMetric1Name() { return "M1"; }
        @Override
        protected String getMetric1Value() { return "V1"; }
        @Override
        protected String getMetric2Name() { return "M2"; }
        @Override
        protected String getMetric2Value() { return "V2"; }

        @Override
        protected void onGameStart() {}
        @Override
        protected void onGameTick() {}
        @Override
        protected void handleTypedChar(char c) {}
        @Override
        protected void handleBackspace() {}
        @Override
        protected void handleSpace() {}
    }
}
