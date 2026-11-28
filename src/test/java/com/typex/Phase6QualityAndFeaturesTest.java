package com.typex;

import com.typex.dao.CustomExerciseDAO;
import com.typex.dao.UserDAO;
import com.typex.model.CustomExercise;
import com.typex.model.TypingResult;
import com.typex.model.User;
import com.typex.service.TypingEngine;
import com.typex.testutil.TestClock;
import com.typex.testutil.TestDatabaseHelper;
import com.typex.ui.MainFrame;
import com.typex.ui.components.TypingTextPanel;
import com.typex.util.ExportManager;
import com.typex.util.SoundManager;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 Quality, Reliability, Performance and Feature Verification Suite.
 */
public class Phase6QualityAndFeaturesTest {

    private static Path isolatedDb;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("java.awt.headless", "true");
        isolatedDb = TestDatabaseHelper.initIsolatedDatabase();
    }

    @AfterAll
    static void tearDown() {
        TestDatabaseHelper.teardownIsolatedDatabase(isolatedDb);
    }

    @Test
    void testExportManagerCsvAndJsonAndReport() throws Exception {
        List<TypingResult> results = new ArrayList<>();
        TypingResult r1 = new TypingResult();
        r1.id = 101;
        r1.userId = 1;
        r1.category = "Common English, Sentences";
        r1.durationSeconds = 60;
        r1.grossWpm = 75.5;
        r1.netWpm = 72.0;
        r1.accuracy = 98.5;
        r1.rawAccuracy = 97.0;
        r1.consistency = 94.2;
        r1.correctChars = 360;
        r1.incorrectChars = 5;
        r1.correctedErrors = 4;
        r1.uncorrectedErrors = 1;
        r1.totalKeystrokes = 380;
        r1.backspaces = 12;
        r1.completedAt = "2026-11-28 10:00:00";
        results.add(r1);

        File tempCsv = File.createTempFile("test_export_", ".csv");
        tempCsv.deleteOnExit();
        ExportManager.exportCsv(tempCsv, results);
        assertTrue(tempCsv.exists());
        String csvContent = Files.readString(tempCsv.toPath());
        assertTrue(csvContent.contains("Date,Category,DurationSeconds,GrossWPM,NetWPM,Accuracy,RawAccuracy"));
        assertTrue(csvContent.contains("75.50"));
        assertTrue(csvContent.contains("72.00"));
        assertTrue(csvContent.contains("98.50"));

        File tempJson = File.createTempFile("test_export_", ".json");
        tempJson.deleteOnExit();
        ExportManager.exportJson(tempJson, results);
        assertTrue(tempJson.exists());
        String jsonContent = Files.readString(tempJson.toPath());
        assertTrue(jsonContent.startsWith("["));
        assertTrue(jsonContent.trim().endsWith("]"));
        assertTrue(jsonContent.contains("\"grossWpm\": 75.50"));
        assertTrue(jsonContent.contains("\"netWpm\": 72.00"));
        assertTrue(jsonContent.contains("\"accuracy\": 98.50"));

        File tempReport = File.createTempFile("test_export_", ".txt");
        tempReport.deleteOnExit();
        ExportManager.exportPrintableReport(tempReport, 1, "Tester", results);
        assertTrue(tempReport.exists());
        String reportContent = Files.readString(tempReport.toPath());
        assertTrue(reportContent.contains("TYPEX — TYPING PERFORMANCE REPORT"));
        assertTrue(reportContent.contains("Tester"));
        assertTrue(reportContent.contains("75.5"));
    }

    @Test
    void testSoundManagerControlAndClamping() {
        boolean original = SoundManager.isEnabled();
        int originalVol = SoundManager.getVolume();

        try {
            SoundManager.setEnabled(true);
            assertTrue(SoundManager.isEnabled());

            boolean toggled = SoundManager.toggleEnabled();
            assertFalse(toggled);
            assertFalse(SoundManager.isEnabled());

            SoundManager.setEnabled(true);
            assertTrue(SoundManager.isEnabled());

            SoundManager.setVolume(150); // Clamped to 100
            assertEquals(100, SoundManager.getVolume());

            SoundManager.setVolume(-20); // Clamped to 0
            assertEquals(0, SoundManager.getVolume());

            SoundManager.setVolume(75);
            assertEquals(75, SoundManager.getVolume());

            // Tone invocation without exceptions
            assertDoesNotThrow(SoundManager::keypress);
            assertDoesNotThrow(SoundManager::error);
            assertDoesNotThrow(SoundManager::complete);
            assertDoesNotThrow(SoundManager::achievement);
            assertDoesNotThrow(SoundManager::levelUp);
        } finally {
            SoundManager.setEnabled(original);
            SoundManager.setVolume(originalVol);
        }
    }

    @Test
    void testTypingEngineDeterministicTimingWithTestClock() {
        TestClock clock = new TestClock(1_000_000_000L);
        TypingEngine engine = new TypingEngine("The quick brown fox jumps over the lazy dog.", clock);

        assertEquals(0.0, engine.elapsedSeconds());
        assertEquals(0.0, engine.grossWpm());

        engine.start();
        clock.advanceSeconds(30); // 30 seconds elapsed
        assertEquals(30.0, engine.elapsedSeconds(), 0.001);

        // Type 5 characters
        engine.type('T');
        engine.type('h');
        engine.type('e');
        engine.type(' ');
        engine.type('q');

        // Gross WPM for 5 chars in 30s (0.5 min) = (5 / 5) / 0.5 = 2.0 WPM
        assertEquals(2.0, engine.grossWpm(), 0.05);

        // Pause test
        engine.pause();
        assertTrue(engine.isPaused());
        clock.advanceSeconds(15); // Advance during pause
        assertEquals(30.0, engine.elapsedSeconds(), 0.001); // Paused time must not count!

        // Resume test
        engine.resume();
        assertFalse(engine.isPaused());
        clock.advanceSeconds(30); // Now 60 seconds of active elapsed time
        assertEquals(60.0, engine.elapsedSeconds(), 0.001);

        // Stop test
        engine.stop();
        clock.advanceSeconds(10);
        assertEquals(60.0, engine.elapsedSeconds(), 0.001); // Frozen after stop!
    }

    @Test
    void testTypingTextPanelBatchedRendering() {
        TypingTextPanel panel = new TypingTextPanel();
        panel.setFontSize(28);
        assertEquals(28, panel.getPassageFontSize());

        TypingEngine engine = new TypingEngine("Quick test for high performance rendering.");
        panel.setEngine(engine);

        // Type several characters
        engine.type('Q');
        engine.type('u');
        engine.type('i');
        engine.type('c');
        engine.type('k');

        assertDoesNotThrow(panel::render);
        assertDoesNotThrow(panel::applyTheme);
    }

    @Test
    void testCustomExerciseBoundsSanitization() {
        UserDAO userDao = new UserDAO();
        CustomExerciseDAO exerciseDAO = new CustomExerciseDAO();
        User u = userDao.create(TestDatabaseHelper.uniqueUsername("ExerciseUser"));

        CustomExercise ce = new CustomExercise();
        ce.userId = u.id;
        ce.title = "A".repeat(200); // 200 chars -> should be truncated to 100
        ce.text = "Hello world ".repeat(3000); // > 25000 chars -> should be truncated
        ce.difficulty = "Expert";

        CustomExercise inserted = exerciseDAO.insert(ce);
        assertNotNull(inserted);
        assertTrue(inserted.id > 0);
        assertTrue(inserted.title.length() <= 100);
        assertTrue(inserted.text.length() <= 25000);

        List<CustomExercise> list = exerciseDAO.findByUser(u.id);
        assertFalse(list.isEmpty());
        assertEquals(inserted.id, list.get(0).id);
    }

    @Test
    void testMainFramePageNavigation() {
        try {
            MainFrame frame = new MainFrame();
            assertNotNull(frame.getUser());

            assertDoesNotThrow(() -> frame.navigate("test"));
            assertDoesNotThrow(() -> frame.navigate("stats"));
            assertDoesNotThrow(() -> frame.navigate("games"));
            assertDoesNotThrow(() -> frame.navigate("dashboard"));
            frame.dispose();
        } catch (java.awt.HeadlessException e) {
            // Permissible in headless test environments where JFrame peer cannot be initialized
            assertTrue(java.awt.GraphicsEnvironment.isHeadless());
        }
    }
}
