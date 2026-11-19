package com.typex;

import com.typex.dao.KeyboardSessionDAO;
import com.typex.dao.UserDAO;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.model.KeyboardSession;
import com.typex.model.User;
import com.typex.ui.components.VirtualKeyboard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KeyboardTrainerTest {

    @BeforeAll
    static void init() throws Exception {
        DatabaseManager.getInstance().initialize();
        DatabaseInitializer.initialize();
    }

    @Test
    void fingerInstructionMapping() {
        assertEquals("Left Index Finger", VirtualKeyboard.getFingerInstruction('f'));
        assertEquals("Right Index Finger", VirtualKeyboard.getFingerInstruction('j'));
        assertEquals("Left Pinky Finger", VirtualKeyboard.getFingerInstruction('a'));
        assertEquals("Right Pinky Finger", VirtualKeyboard.getFingerInstruction(';'));
        assertEquals("Both Thumbs (Spacebar)", VirtualKeyboard.getFingerInstruction(' '));
    }

    @Test
    void keyboardSessionPersistence() {
        UserDAO userDao = new UserDAO();
        KeyboardSessionDAO sessionDao = new KeyboardSessionDAO();

        User u = userDao.create("KeyboardTester_" + System.currentTimeMillis());

        KeyboardSession s = new KeyboardSession();
        s.userId = u.id;
        s.drillType = "Home Row (ASDF JKL;)";
        s.keysTested = 30;
        s.correctKeys = 29;
        s.accuracy = 96.67;
        s.avgReactionTimeMs = 285.5;

        int id = sessionDao.insert(s);
        assertTrue(id > 0);

        List<KeyboardSession> list = sessionDao.findByUser(u.id, 5);
        assertFalse(list.isEmpty());
        assertEquals("Home Row (ASDF JKL;)", list.get(0).drillType);
        assertEquals(30, list.get(0).keysTested);
        assertEquals(285.5, list.get(0).avgReactionTimeMs, 0.1);

        userDao.delete(u.id);
    }
}
