package com.typex.service;

import com.typex.util.KeyboardLayout;
import com.typex.util.KeyboardLayout.Finger;
import com.typex.util.KeyboardLayout.KeyDef;
import com.typex.util.KeyboardLayout.LayoutInfo;

import java.util.*;

/**
 * Generates dynamic, layout-aware character pools for keyboard drills.
 * Evaluates the active LayoutInfo and KeyDef finger mappings directly rather
 * than hardcoding QWERTY strings.
 */
public final class KeyboardDrillService {

    public static final String[] DRILLS = {
        "Random Keys",
        "Home Row",
        "Top Row",
        "Bottom Row",
        "Left Hand",
        "Right Hand",
        "Left Index Finger",
        "Right Index Finger",
        "Index Fingers",
        "Middle Fingers",
        "Ring Fingers",
        "Pinky Fingers",
        "Weak Keys Drill"
    };

    private KeyboardDrillService() {}

    /**
     * Dynamically builds a character drill pool for the specified layout and drill name.
     */
    public static String getDrillPool(LayoutInfo layout, String drillName) {
        if (layout == null) {
            layout = KeyboardLayout.getLayout("US_QWERTY");
        }
        if (drillName == null) {
            drillName = "Random Keys";
        }

        String lower = drillName.trim().toLowerCase();
        Set<Character> chars = new LinkedHashSet<>();

        if (lower.startsWith("home row")) {
            // Row 2 is standard Home Row
            if (layout.rows.size() > 2) {
                addNonModifierChars(layout.rows.get(2), chars);
            }
        } else if (lower.startsWith("top row")) {
            // Row 1 is Top Row
            if (layout.rows.size() > 1) {
                addNonModifierChars(layout.rows.get(1), chars);
            }
        } else if (lower.startsWith("bottom row")) {
            // Row 3 is Bottom Row
            if (layout.rows.size() > 3) {
                addNonModifierChars(layout.rows.get(3), chars);
            }
        } else if (lower.equals("left hand")) {
            for (List<KeyDef> row : layout.rows) {
                for (KeyDef k : row) {
                    if (isLeftHandFinger(k.finger)) {
                        addKeyChar(k, chars);
                    }
                }
            }
        } else if (lower.equals("right hand")) {
            for (List<KeyDef> row : layout.rows) {
                for (KeyDef k : row) {
                    if (isRightHandFinger(k.finger)) {
                        addKeyChar(k, chars);
                    }
                }
            }
        } else if (lower.equals("left index finger")) {
            filterByFingers(layout, Set.of(Finger.LEFT_INDEX), chars);
        } else if (lower.equals("right index finger")) {
            filterByFingers(layout, Set.of(Finger.RIGHT_INDEX), chars);
        } else if (lower.equals("index fingers")) {
            filterByFingers(layout, Set.of(Finger.LEFT_INDEX, Finger.RIGHT_INDEX), chars);
        } else if (lower.equals("middle fingers")) {
            filterByFingers(layout, Set.of(Finger.LEFT_MIDDLE, Finger.RIGHT_MIDDLE), chars);
        } else if (lower.equals("ring fingers")) {
            filterByFingers(layout, Set.of(Finger.LEFT_RING, Finger.RIGHT_RING), chars);
        } else if (lower.equals("pinky fingers")) {
            filterByFingers(layout, Set.of(Finger.LEFT_PINKY, Finger.RIGHT_PINKY), chars);
        } else if (lower.startsWith("weak keys")) {
            // Fallback for Weak Keys when user has no logged errors
            return getDrillPool(layout, "Home Row");
        } else {
            // "Random Keys" or default: include all letter and printable keys from rows 1, 2, 3
            for (int r = 1; r <= Math.min(3, layout.rows.size() - 1); r++) {
                addNonModifierChars(layout.rows.get(r), chars);
            }
            if (chars.isEmpty()) {
                for (List<KeyDef> row : layout.rows) {
                    addNonModifierChars(row, chars);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }

        if (sb.length() == 0) {
            // Layout-aware fallback: extract all printable non-modifier characters from the layout
            for (List<KeyDef> row : layout.rows) {
                addNonModifierChars(row, chars);
            }
            for (char c : chars) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Filters historical error characters against the active layout.
     * Retains only characters supported by the layout. If none remain,
     * cleanly falls back to the active layout's dynamic Home Row pool.
     */
    public static String filterWeakKeys(LayoutInfo layout, Collection<Character> errorChars) {
        if (layout == null) layout = KeyboardLayout.getLayout("US_QWERTY");
        if (errorChars == null || errorChars.isEmpty()) {
            return getDrillPool(layout, "Home Row");
        }

        Set<Character> valid = new LinkedHashSet<>();
        for (Character ch : errorChars) {
            if (ch == null) continue;
            char c = Character.toLowerCase(ch);
            if (layout.containsChar(c)) {
                valid.add(c);
            }
        }

        if (valid.isEmpty()) {
            return getDrillPool(layout, "Home Row");
        }

        StringBuilder sb = new StringBuilder();
        for (char c : valid) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isLeftHandFinger(Finger f) {
        return f == Finger.LEFT_PINKY || f == Finger.LEFT_RING || f == Finger.LEFT_MIDDLE || f == Finger.LEFT_INDEX;
    }

    private static boolean isRightHandFinger(Finger f) {
        return f == Finger.RIGHT_INDEX || f == Finger.RIGHT_MIDDLE || f == Finger.RIGHT_RING || f == Finger.RIGHT_PINKY;
    }

    private static void filterByFingers(LayoutInfo layout, Set<Finger> targetFingers, Set<Character> out) {
        for (List<KeyDef> row : layout.rows) {
            for (KeyDef k : row) {
                if (k.finger != null && targetFingers.contains(k.finger)) {
                    addKeyChar(k, out);
                }
            }
        }
    }

    private static void addNonModifierChars(List<KeyDef> row, Set<Character> out) {
        for (KeyDef k : row) {
            addKeyChar(k, out);
        }
    }

    private static void addKeyChar(KeyDef k, Set<Character> out) {
        if (k == null || k.isModifier) return;
        if (k.id.equalsIgnoreCase("SPACE") || k.finger == Finger.THUMB) return;

        if (k.primary != null && k.primary.length() == 1) {
            char p = k.primary.charAt(0);
            if (p >= 32) {
                out.add(Character.toLowerCase(p));
            }
        }
    }
}
