package com.typex;

import com.typex.config.ThemeManager;
import com.typex.dao.UserPreferenceDAO;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.model.UserPreference;
import com.typex.ui.components.TypingTextPanel;
import com.typex.ui.components.VirtualKeyboard;
import com.typex.util.KeyboardLayout;
import com.typex.util.KeyboardLayout.Finger;
import com.typex.util.KeyboardLayout.LayoutInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class Phase4DesignAndLayoutTest {

    @BeforeAll
    static void init() throws Exception {
        System.setProperty("java.awt.headless", "true");
        DatabaseManager.getInstance().initialize();
        DatabaseInitializer.initialize();
    }

    @Test
    void testThemeManagerLightTokensAndContrast() {
        // Test light theme defaults and tokens
        ThemeManager.apply(false, new Color(0x2563EB));
        assertFalse(ThemeManager.isDark(), "Should be in light theme mode");
        assertEquals(new Color(0x2563EB), ThemeManager.accent());
        assertEquals(Color.WHITE, ThemeManager.bgCard());
        assertEquals(Color.WHITE, ThemeManager.fgOnAccent(), "Button text on blue accent must be white");

        // Verify semantic colors
        assertNotNull(ThemeManager.colorSuccess());
        assertNotNull(ThemeManager.colorWarning());
        assertNotNull(ThemeManager.colorError());
        assertNotNull(ThemeManager.colorGold());
        assertNotNull(ThemeManager.borderColor());

        // Test dark mode
        ThemeManager.apply(true, new Color(0x2563EB));
        assertTrue(ThemeManager.isDark());
        assertNotEquals(Color.WHITE, ThemeManager.bgCard());
    }

    @Test
    void testAllKeyboardLayoutsRegisteredAndResolvable() {
        Collection<LayoutInfo> layouts = KeyboardLayout.getAvailableLayouts();
        assertTrue(layouts.size() >= 6, "Must support at least 6 keyboard layouts");

        String[] expectedIds = {"US_QWERTY", "UK_QWERTY", "GERMAN_QWERTZ", "FRENCH_AZERTY", "DVORAK", "COLEMAK"};
        for (String id : expectedIds) {
            LayoutInfo info = KeyboardLayout.getLayout(id);
            assertNotNull(info, "Layout " + id + " must exist");
            assertNotNull(info.displayName);
            assertFalse(info.rows.isEmpty(), "Layout " + id + " must have key rows");
        }

        // Test case-insensitive lookup
        LayoutInfo qwertz = KeyboardLayout.getLayoutOrDefault("German QWERTZ");
        assertNotNull(qwertz);
        assertEquals("GERMAN_QWERTZ", qwertz.id);

        LayoutInfo azerty = KeyboardLayout.getLayoutOrDefault("French AZERTY");
        assertNotNull(azerty);
        assertEquals("FRENCH_AZERTY", azerty.id);
    }

    @Test
    void testKeyboardLayoutFingerGuidance() {
        LayoutInfo us = KeyboardLayout.getLayout("US_QWERTY");
        assertEquals(Finger.LEFT_INDEX, us.getFingerForChar('f'));
        assertEquals(Finger.RIGHT_INDEX, us.getFingerForChar('j'));
        assertEquals(Finger.LEFT_PINKY, us.getFingerForChar('a'));
        assertEquals(Finger.RIGHT_PINKY, us.getFingerForChar(';'));
        assertEquals(Finger.THUMB, us.getFingerForChar(' '));

        // German QWERTZ swap (Z and Y)
        LayoutInfo de = KeyboardLayout.getLayout("DE_QWERTZ");
        assertNotNull(de.findKeyForChar('z'));
        assertNotNull(de.findKeyForChar('y'));

        // French AZERTY (A and Q swap)
        LayoutInfo fr = KeyboardLayout.getLayout("FR_AZERTY");
        assertNotNull(fr.findKeyForChar('a'));
        assertNotNull(fr.findKeyForChar('q'));
    }

    @Test
    void testVirtualKeyboardAndFingerInstructions() {
        String fInst = VirtualKeyboard.getFingerInstruction('f');
        assertTrue(fInst.contains("Index"), "Instruction for 'f' must mention index finger: " + fInst);

        String spaceInst = VirtualKeyboard.getFingerInstruction(' ');
        assertTrue(spaceInst.contains("Spacebar") || spaceInst.contains("Thumb"), "Instruction for space: " + spaceInst);
    }

    @Test
    void testTypingTextPanelLargePassageFontSizes() {
        TypingTextPanel panel = new TypingTextPanel();
        assertEquals(26, panel.getPassageFontSize(), "Default passage font size must be 26px");

        panel.setPassageFontSize(32);
        assertEquals(32, panel.getPassageFontSize());

        // Bounded checks (18 - 38px)
        panel.setPassageFontSize(10); // Below bounds, should not change
        assertEquals(32, panel.getPassageFontSize());

        panel.setPassageFontSize(50); // Above bounds, should not change
        assertEquals(32, panel.getPassageFontSize());

        panel.setPassageFontSize(22);
        assertEquals(22, panel.getPassageFontSize());
    }

    @Test
    void testUserPreferenceKeyboardLayoutPersistence() {
        com.typex.dao.UserDAO userDAO = new com.typex.dao.UserDAO();
        com.typex.model.User u = userDAO.create("Phase4Tester_" + System.currentTimeMillis());
        assertNotNull(u);

        UserPreferenceDAO dao = new UserPreferenceDAO();
        UserPreference p = new UserPreference(u.id);
        p.keyboardLayout = "German QWERTZ";
        p.passageFontSize = 28;

        dao.save(p);
        UserPreference retrieved = dao.getForUser(u.id);
        assertNotNull(retrieved);
        assertEquals("German QWERTZ", retrieved.keyboardLayout);
        assertEquals(28, retrieved.passageFontSize);
    }

    @Test
    void testSystemLayoutDetection() {
        String detected = KeyboardLayout.detectSystemLayout();
        assertNotNull(detected);
        assertFalse(detected.isEmpty());
        assertTrue(detected.contains("Locale") || detected.contains("Default") || detected.contains("QWERTY"));
    }
}
