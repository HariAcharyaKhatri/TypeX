package com.typex;

import com.typex.dao.*;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.model.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;

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
        // Simulate try-with-resources closing the connection proxy
        c1.close();
        // The underlying connection must still be open and usable
        Connection c2 = DatabaseManager.getInstance().getConnection();
        assertFalse(c2.isClosed());
        try (var st = c2.createStatement(); var rs = st.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @Order(2)
    void userCreationAndProfileSwitching() {
        UserDAO dao = new UserDAO();
        String testUsername = "TestUser_" + System.currentTimeMillis();

        User created = dao.create(testUsername);
        assertNotNull(created);
        assertEquals(testUsername, created.username);
        assertTrue(created.id > 0);

        // Update profile
        created.skillLevel = "Advanced";
        created.dailyGoal = 45;
        dao.update(created);

        User fetched = dao.findById(created.id);
        assertNotNull(fetched);
        assertEquals("Advanced", fetched.skillLevel);
        assertEquals(45, fetched.dailyGoal);

        // Cleanup
        dao.delete(created.id);
        assertNull(dao.findById(created.id));
    }

    @Test
    @Order(3)
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
        r.correctChars = 245;
        r.incorrectChars = 5;
        r.totalChars = 250;
        r.totalKeystrokes = 255;
        r.backspaces = 5;
        r.errorPercent = 2.0;

        int resId = resultDao.insert(r);
        assertTrue(resId > 0);

        TypingResult best = resultDao.findBestByUser(u.id);
        assertNotNull(best);
        assertEquals(50.0, best.netWpm, 0.01);

        statsDao.recordTest(u.id, r.netWpm, r.accuracy, r.durationSeconds, 50);
        assertEquals(1, statsDao.currentStreak(u.id));
        assertEquals(60, statsDao.totalPracticeSeconds(u.id));

        // Cleanup
        userDao.delete(u.id);
    }

    @Test
    @Order(4)
    void achievementUnlockingNoDuplicates() {
        UserDAO userDao = new UserDAO();
        AchievementDAO achDao = new AchievementDAO();

        User u = userDao.create("AchTester_" + System.currentTimeMillis());

        // First unlock should succeed
        boolean firstUnlock = achDao.unlock(u.id, "FIRST_TEST");
        assertTrue(firstUnlock);

        // Second unlock should be ignored (no duplicates)
        boolean secondUnlock = achDao.unlock(u.id, "FIRST_TEST");
        assertFalse(secondUnlock);

        List<Achievement> all = achDao.allForUser(u.id);
        long unlockedCount = all.stream().filter(a -> a.code.equals("FIRST_TEST") && a.unlocked).count();
        assertEquals(1, unlockedCount);

        userDao.delete(u.id);
    }
}
