package com.typex.util;

import java.awt.Color;
import java.awt.im.InputContext;
import java.util.*;

/**
 * Keyboard Layout definitions and input detection.
 * Supports US QWERTY, UK QWERTY, German QWERTZ, French AZERTY, Dvorak, and Colemak.
 */
public class KeyboardLayout {

    public enum Finger {
        LEFT_PINKY("Left Pinky Finger", new Color(0xF43F5E)),
        LEFT_RING("Left Ring Finger", new Color(0xFB923C)),
        LEFT_MIDDLE("Left Middle Finger", new Color(0xFBBF24)),
        LEFT_INDEX("Left Index Finger", new Color(0x10B981)),
        RIGHT_INDEX("Right Index Finger", new Color(0x06B6D4)),
        RIGHT_MIDDLE("Right Middle Finger", new Color(0x3B82F6)),
        RIGHT_RING("Right Ring Finger", new Color(0x8B5CF6)),
        RIGHT_PINKY("Right Pinky Finger", new Color(0xEC4899)),
        THUMB("Both Thumbs (Spacebar)", new Color(0x94A3B8));

        public final String displayName;
        public final Color color;

        Finger(String name, Color c) {
            this.displayName = name;
            this.color = c;
        }
    }

    public static class KeyDef {
        public final String id;
        public final String primary;
        public final String shifted;
        public final double widthMultiplier;
        public final Finger finger;
        public final boolean isModifier;

        public KeyDef(String id, String primary, String shifted, double width, Finger finger, boolean isModifier) {
            this.id = id;
            this.primary = primary;
            this.shifted = shifted;
            this.widthMultiplier = width;
            this.finger = finger;
            this.isModifier = isModifier;
        }

        public KeyDef(String id, String primary, double width, Finger finger, boolean isModifier) {
            this(id, primary, null, width, finger, isModifier);
        }
    }

    public static class LayoutInfo {
        public final String id;
        public final String displayName;
        public final String description;
        public final List<List<KeyDef>> rows;
        private final Map<Character, KeyDef> charToKey = new HashMap<>();
        private final Map<Character, Boolean> charRequiresShift = new HashMap<>();

        public LayoutInfo(String id, String displayName, String description, List<List<KeyDef>> rows) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.rows = rows;
            indexKeys();
        }

        private void indexKeys() {
            for (List<KeyDef> row : rows) {
                for (KeyDef k : row) {
                    if (k.primary != null && k.primary.length() == 1) {
                        char p = k.primary.charAt(0);
                        charToKey.put(Character.toLowerCase(p), k);
                        charRequiresShift.put(Character.toLowerCase(p), false);
                        if (Character.isLetter(p)) {
                            charToKey.put(Character.toUpperCase(p), k);
                            charRequiresShift.put(Character.toUpperCase(p), true);
                        }
                    }
                    if (k.shifted != null && k.shifted.length() == 1) {
                        char s = k.shifted.charAt(0);
                        charToKey.put(s, k);
                        charRequiresShift.put(s, true);
                    }
                }
            }
            // Spacebar
            KeyDef space = new KeyDef("SPACE", " ", null, 6.2, Finger.THUMB, false);
            charToKey.put(' ', space);
            charRequiresShift.put(' ', false);
        }

        public KeyDef findKeyForChar(char c) {
            return charToKey.get(c);
        }

        public boolean requiresShift(char c) {
            return charRequiresShift.getOrDefault(c, false);
        }

        public Finger getFingerForChar(char c) {
            KeyDef k = findKeyForChar(c);
            return k != null ? k.finger : Finger.THUMB;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class DetectionResult {
        public final LayoutInfo layout;
        public final String reason;
        public final boolean isGuaranteed;

        public DetectionResult(LayoutInfo layout, String reason, boolean isGuaranteed) {
            this.layout = layout;
            this.reason = reason;
            this.isGuaranteed = isGuaranteed;
        }
    }

    public static final String[] LAYOUT_NAMES = {
        "US QWERTY", "UK QWERTY", "German QWERTZ", "French AZERTY", "Dvorak", "Colemak"
    };

    public static String detectSystemLayout() {
        DetectionResult res = detectOperatingSystemLayout();
        return res.layout.displayName + " (" + res.reason + ")";
    }

    private static final Map<String, LayoutInfo> ALL_LAYOUTS = new LinkedHashMap<>();

    static {
        registerLayout(buildUsQwerty());
        registerLayout(buildUkQwerty());
        registerLayout(buildGermanQwertz());
        registerLayout(buildFrenchAzerty());
        registerLayout(buildDvorak());
        registerLayout(buildColemak());
    }

    private static void registerLayout(LayoutInfo l) {
        ALL_LAYOUTS.put(l.id, l);
    }

    public static Collection<LayoutInfo> getAvailableLayouts() {
        return ALL_LAYOUTS.values();
    }

    public static LayoutInfo getLayout(String idOrName) {
        return getLayoutOrDefault(idOrName);
    }

    public static LayoutInfo getLayoutOrDefault(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            return ALL_LAYOUTS.get("US_QWERTY");
        }
        String clean = idOrName.trim();

        // 1. Direct ID or Display Name match
        for (LayoutInfo l : ALL_LAYOUTS.values()) {
            if (l.id.equalsIgnoreCase(clean) || l.displayName.equalsIgnoreCase(clean)) {
                return l;
            }
        }

        // 2. Normalized Aliases
        String upper = clean.toUpperCase().replace(" ", "_").replace("-", "_");
        if (upper.contains("GERMAN") || upper.equals("DE_QWERTZ") || upper.equals("QWERTZ")) {
            return ALL_LAYOUTS.get("GERMAN_QWERTZ");
        }
        if (upper.contains("FRENCH") || upper.equals("FR_AZERTY") || upper.equals("AZERTY")) {
            return ALL_LAYOUTS.get("FRENCH_AZERTY");
        }
        if (upper.contains("UK") || upper.equals("EN_GB")) {
            return ALL_LAYOUTS.get("UK_QWERTY");
        }
        if (upper.contains("DVORAK")) {
            return ALL_LAYOUTS.get("DVORAK");
        }
        if (upper.contains("COLEMAK")) {
            return ALL_LAYOUTS.get("COLEMAK");
        }
        if (upper.contains("QWERTY") || upper.equals("US")) {
            return ALL_LAYOUTS.get("US_QWERTY");
        }

        return ALL_LAYOUTS.get("US_QWERTY");
    }

    public static String getFingerInstruction(LayoutInfo layout, char c) {
        if (c == ' ') return "Both Thumbs (Spacebar)";
        if (layout == null) layout = ALL_LAYOUTS.get("US_QWERTY");

        Finger f = layout.getFingerForChar(c);
        if (f == null) {
            // Check lowercase or uppercase fallback
            if (Character.isLetter(c)) {
                f = layout.getFingerForChar(Character.toLowerCase(c));
            }
        }

        if (f == null) {
            LayoutInfo us = ALL_LAYOUTS.get("US_QWERTY");
            f = (us != null) ? us.getFingerForChar(c) : null;
        }

        if (f == null) {
            return "Target Finger: Any Free Finger";
        }

        boolean shifted = layout.requiresShift(c);
        if (!shifted && Character.isUpperCase(c)) {
            shifted = true;
        }

        String shiftNote = "";
        if (shifted) {
            boolean isLeftHand = f == Finger.LEFT_PINKY || f == Finger.LEFT_RING || f == Finger.LEFT_MIDDLE || f == Finger.LEFT_INDEX;
            shiftNote = " + " + (isLeftHand ? "Right Shift" : "Left Shift");
        }

        return f.displayName + shiftNote;
    }

    public static DetectionResult detectOperatingSystemLayout() {
        Locale loc = Locale.getDefault();
        try {
            InputContext ic = InputContext.getInstance();
            if (ic != null && ic.getLocale() != null) {
                loc = ic.getLocale();
            }
        } catch (Exception ignored) {}

        String lang = loc.getLanguage().toLowerCase();
        String country = loc.getCountry().toUpperCase();

        if ("de".equals(lang)) {
            return new DetectionResult(ALL_LAYOUTS.get("GERMAN_QWERTZ"),
                    "Detected German input language (" + loc.getDisplayName() + ")", false);
        } else if ("fr".equals(lang)) {
            return new DetectionResult(ALL_LAYOUTS.get("FRENCH_AZERTY"),
                    "Detected French input language (" + loc.getDisplayName() + ")", false);
        } else if ("en".equals(lang) && "GB".equals(country)) {
            return new DetectionResult(ALL_LAYOUTS.get("UK_QWERTY"),
                    "Detected United Kingdom input locale (" + loc.getDisplayName() + ")", false);
        } else if ("en".equals(lang)) {
            return new DetectionResult(ALL_LAYOUTS.get("US_QWERTY"),
                    "Detected US English input locale (" + loc.getDisplayName() + ")", false);
        }
        return new DetectionResult(ALL_LAYOUTS.get("US_QWERTY"),
                "Active locale is " + loc.getDisplayName() + " (Physical hardware layout cannot be verified directly)", false);
    }

    // --- Layout Builders ---

    private static LayoutInfo buildUsQwerty() {
        List<List<KeyDef>> rows = new ArrayList<>();

        // Row 0: Numbers
        rows.add(List.of(
                new KeyDef("GRAVE", "`", "~", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "1", "!", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "2", "@", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "3", "#", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "4", "$", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "5", "%", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "6", "^", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "7", "&", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "8", "*", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "9", "(", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "0", ")", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("MINUS", "-", "_", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("EQUALS", "=", "+", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        // Row 1: QWERTY
        rows.add(List.of(
                new KeyDef("TAB", "Tab", 1.5, Finger.LEFT_PINKY, true),
                new KeyDef("Q", "q", "Q", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("W", "w", "W", 1.0, Finger.LEFT_RING, false),
                new KeyDef("E", "e", "E", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("R", "r", "R", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("T", "t", "T", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("Y", "y", "Y", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("U", "u", "U", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("I", "i", "I", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("O", "o", "O", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("P", "p", "P", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("LBRACKET", "[", "{", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RBRACKET", "]", "}", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSLASH", "\\", "|", 1.5, Finger.RIGHT_PINKY, false)
        ));

        // Row 2: ASDF
        rows.add(List.of(
                new KeyDef("CAPS", "Caps Lock", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("S", "s", "S", 1.0, Finger.LEFT_RING, false),
                new KeyDef("D", "d", "D", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("F", "f", "F", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("J", "j", "J", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("K", "k", "K", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("SEMICOLON", ";", ":", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("QUOTE", "'", "\"", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Enter", 2.2, Finger.RIGHT_PINKY, true)
        ));

        // Row 3: ZXCV
        rows.add(List.of(
                new KeyDef("LSHIFT", "Shift", 2.3, Finger.LEFT_PINKY, true),
                new KeyDef("Z", "z", "Z", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("X", "x", "X", 1.0, Finger.LEFT_RING, false),
                new KeyDef("C", "c", "C", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("V", "v", "V", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("B", "b", "B", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("N", "n", "N", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("M", "m", "M", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("COMMA", ",", "<", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("PERIOD", ".", ">", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("SLASH", "/", "?", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RSHIFT", "Shift", 2.7, Finger.RIGHT_PINKY, true)
        ));

        // Row 4: Space row
        rows.add(List.of(
                new KeyDef("LCTRL", "Ctrl", 1.4, Finger.LEFT_PINKY, true),
                new KeyDef("LWIN", "Win", 1.2, Finger.LEFT_PINKY, true),
                new KeyDef("LALT", "Alt", 1.3, Finger.LEFT_RING, true),
                new KeyDef("SPACE", "Space", 6.2, Finger.THUMB, false),
                new KeyDef("RALT", "Alt", 1.3, Finger.RIGHT_RING, true),
                new KeyDef("RWIN", "Fn", 1.2, Finger.RIGHT_PINKY, true),
                new KeyDef("RCTRL", "Ctrl", 1.4, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("US_QWERTY", "US QWERTY", "Standard American QWERTY keyboard layout", rows);
    }

    private static LayoutInfo buildUkQwerty() {
        LayoutInfo base = buildUsQwerty();
        List<List<KeyDef>> rows = new ArrayList<>(base.rows);

        // UK modifies shift on 2 ("), 3 (£), and ' (@)
        rows.set(0, List.of(
                new KeyDef("GRAVE", "`", "¬", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "1", "!", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "2", "\"", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "3", "£", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "4", "$", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "5", "%", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "6", "^", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "7", "&", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "8", "*", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "9", "(", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "0", ")", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("MINUS", "-", "_", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("EQUALS", "=", "+", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        rows.set(2, List.of(
                new KeyDef("CAPS", "Caps Lock", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("S", "s", "S", 1.0, Finger.LEFT_RING, false),
                new KeyDef("D", "d", "D", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("F", "f", "F", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("J", "j", "J", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("K", "k", "K", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("SEMICOLON", ";", ":", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("QUOTE", "'", "@", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Enter", 2.2, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("UK_QWERTY", "UK QWERTY", "United Kingdom QWERTY layout with £ and inverted @ / \" symbols", rows);
    }

    private static LayoutInfo buildGermanQwertz() {
        List<List<KeyDef>> rows = new ArrayList<>();

        // Row 0: Numbers
        rows.add(List.of(
                new KeyDef("CARET", "^", "°", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "1", "!", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "2", "\"", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "3", "§", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "4", "$", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "5", "%", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "6", "&", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "7", "/", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "8", "(", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "9", ")", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "0", "=", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("SZ", "ß", "?", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ACUTE", "´", "`", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        // Row 1: QWERTZ
        rows.add(List.of(
                new KeyDef("TAB", "Tab", 1.5, Finger.LEFT_PINKY, true),
                new KeyDef("Q", "q", "Q", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("W", "w", "W", 1.0, Finger.LEFT_RING, false),
                new KeyDef("E", "e", "E", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("R", "r", "R", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("T", "t", "T", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("Z", "z", "Z", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("U", "u", "U", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("I", "i", "I", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("O", "o", "O", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("P", "p", "P", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("UE", "ü", "Ü", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("PLUS", "+", "*", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("HASH", "#", "'", 1.5, Finger.RIGHT_PINKY, false)
        ));

        // Row 2: ASDF
        rows.add(List.of(
                new KeyDef("CAPS", "Caps Lock", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("S", "s", "S", 1.0, Finger.LEFT_RING, false),
                new KeyDef("D", "d", "D", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("F", "f", "F", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("J", "j", "J", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("K", "k", "K", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("OE", "ö", "Ö", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("AE", "ä", "Ä", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Enter", 2.2, Finger.RIGHT_PINKY, true)
        ));

        // Row 3: YXCV
        rows.add(List.of(
                new KeyDef("LSHIFT", "Shift", 2.3, Finger.LEFT_PINKY, true),
                new KeyDef("Y", "y", "Y", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("X", "x", "X", 1.0, Finger.LEFT_RING, false),
                new KeyDef("C", "c", "C", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("V", "v", "V", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("B", "b", "B", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("N", "n", "N", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("M", "m", "M", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("COMMA", ",", ";", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("PERIOD", ".", ":", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("MINUS", "-", "_", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RSHIFT", "Shift", 2.7, Finger.RIGHT_PINKY, true)
        ));

        // Row 4: Space row
        rows.add(List.of(
                new KeyDef("LCTRL", "Strg", 1.4, Finger.LEFT_PINKY, true),
                new KeyDef("LWIN", "Win", 1.2, Finger.LEFT_PINKY, true),
                new KeyDef("LALT", "Alt", 1.3, Finger.LEFT_RING, true),
                new KeyDef("SPACE", "Leertaste", 6.2, Finger.THUMB, false),
                new KeyDef("RALT", "AltGr", 1.3, Finger.RIGHT_RING, true),
                new KeyDef("RWIN", "Fn", 1.2, Finger.RIGHT_PINKY, true),
                new KeyDef("RCTRL", "Strg", 1.4, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("GERMAN_QWERTZ", "German QWERTZ", "Standard German QWERTZ layout with umlauts ä, ö, ü and ß", rows);
    }

    private static LayoutInfo buildFrenchAzerty() {
        List<List<KeyDef>> rows = new ArrayList<>();

        // Row 0: Numbers / symbols
        rows.add(List.of(
                new KeyDef("SQUARE", "²", "", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "&", "1", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "é", "2", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "\"", "3", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "'", "4", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "(", "5", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "-", "6", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "è", "7", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "_", "8", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "ç", "9", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "à", "0", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RPAREN", ")", "°", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("EQUAL", "=", "+", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        // Row 1: AZERTY
        rows.add(List.of(
                new KeyDef("TAB", "Tab", 1.5, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("Z", "z", "Z", 1.0, Finger.LEFT_RING, false),
                new KeyDef("E", "e", "E", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("R", "r", "R", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("T", "t", "T", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("Y", "y", "Y", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("U", "u", "U", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("I", "i", "I", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("O", "o", "O", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("P", "p", "P", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("CIRCUM", "^", "¨", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("DOLLAR", "$", "£", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ASTERISK", "*", "µ", 1.5, Finger.RIGHT_PINKY, false)
        ));

        // Row 2: QSDF
        rows.add(List.of(
                new KeyDef("CAPS", "Verr Maj", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("Q", "q", "Q", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("S", "s", "S", 1.0, Finger.LEFT_RING, false),
                new KeyDef("D", "d", "D", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("F", "f", "F", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("J", "j", "J", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("K", "k", "K", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("M", "m", "M", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("UGRAVE", "ù", "%", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Entrée", 2.2, Finger.RIGHT_PINKY, true)
        ));

        // Row 3: WXCV
        rows.add(List.of(
                new KeyDef("LSHIFT", "Maj", 2.3, Finger.LEFT_PINKY, true),
                new KeyDef("W", "w", "W", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("X", "x", "X", 1.0, Finger.LEFT_RING, false),
                new KeyDef("C", "c", "C", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("V", "v", "V", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("B", "b", "B", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("N", "n", "N", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("COMMA", ",", "?", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("SEMICOLON", ";", ".", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("COLON", ":", "/", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("EXCLAM", "!", "§", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RSHIFT", "Maj", 2.7, Finger.RIGHT_PINKY, true)
        ));

        // Row 4: Space row
        rows.add(List.of(
                new KeyDef("LCTRL", "Ctrl", 1.4, Finger.LEFT_PINKY, true),
                new KeyDef("LWIN", "Win", 1.2, Finger.LEFT_PINKY, true),
                new KeyDef("LALT", "Alt", 1.3, Finger.LEFT_RING, true),
                new KeyDef("SPACE", "Espace", 6.2, Finger.THUMB, false),
                new KeyDef("RALT", "AltGr", 1.3, Finger.RIGHT_RING, true),
                new KeyDef("RWIN", "Fn", 1.2, Finger.RIGHT_PINKY, true),
                new KeyDef("RCTRL", "Ctrl", 1.4, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("FRENCH_AZERTY", "French AZERTY", "Standard French AZERTY keyboard layout", rows);
    }

    private static LayoutInfo buildDvorak() {
        List<List<KeyDef>> rows = new ArrayList<>();

        // Row 0
        rows.add(List.of(
                new KeyDef("GRAVE", "`", "~", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "1", "!", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "2", "@", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "3", "#", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "4", "$", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "5", "%", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "6", "^", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "7", "&", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "8", "*", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "9", "(", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "0", ")", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("LBRACKET", "[", "{", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RBRACKET", "]", "}", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        // Row 1: ',.PYFGCRL/=
        rows.add(List.of(
                new KeyDef("TAB", "Tab", 1.5, Finger.LEFT_PINKY, true),
                new KeyDef("QUOTE", "'", "\"", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("COMMA", ",", "<", 1.0, Finger.LEFT_RING, false),
                new KeyDef("PERIOD", ".", ">", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("P", "p", "P", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("Y", "y", "Y", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("F", "f", "F", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("C", "c", "C", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("R", "r", "R", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("SLASH", "/", "?", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("EQUALS", "=", "+", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSLASH", "\\", "|", 1.5, Finger.RIGHT_PINKY, false)
        ));

        // Row 2: AOEUIDHTNS-
        rows.add(List.of(
                new KeyDef("CAPS", "Caps Lock", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("O", "o", "O", 1.0, Finger.LEFT_RING, false),
                new KeyDef("E", "e", "E", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("U", "u", "U", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("I", "i", "I", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("D", "d", "D", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("T", "t", "T", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("N", "n", "N", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("S", "s", "S", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("MINUS", "-", "_", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Enter", 2.2, Finger.RIGHT_PINKY, true)
        ));

        // Row 3: ;QJKXBMWVZ
        rows.add(List.of(
                new KeyDef("LSHIFT", "Shift", 2.3, Finger.LEFT_PINKY, true),
                new KeyDef("SEMICOLON", ";", ":", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("Q", "q", "Q", 1.0, Finger.LEFT_RING, false),
                new KeyDef("J", "j", "J", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("K", "k", "K", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("X", "x", "X", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("B", "b", "B", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("M", "m", "M", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("W", "w", "W", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("V", "v", "V", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("Z", "z", "Z", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RSHIFT", "Shift", 2.7, Finger.RIGHT_PINKY, true)
        ));

        // Row 4: Space row
        rows.add(List.of(
                new KeyDef("LCTRL", "Ctrl", 1.4, Finger.LEFT_PINKY, true),
                new KeyDef("LWIN", "Win", 1.2, Finger.LEFT_PINKY, true),
                new KeyDef("LALT", "Alt", 1.3, Finger.LEFT_RING, true),
                new KeyDef("SPACE", "Space", 6.2, Finger.THUMB, false),
                new KeyDef("RALT", "Alt", 1.3, Finger.RIGHT_RING, true),
                new KeyDef("RWIN", "Fn", 1.2, Finger.RIGHT_PINKY, true),
                new KeyDef("RCTRL", "Ctrl", 1.4, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("DVORAK", "Dvorak", "Dvorak Simplified Keyboard layout designed for ergonomics and reduced finger travel", rows);
    }

    private static LayoutInfo buildColemak() {
        List<List<KeyDef>> rows = new ArrayList<>();

        // Row 0
        rows.add(List.of(
                new KeyDef("GRAVE", "`", "~", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("1", "1", "!", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("2", "2", "@", 1.0, Finger.LEFT_RING, false),
                new KeyDef("3", "3", "#", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("4", "4", "$", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("5", "5", "%", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("6", "6", "^", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("7", "7", "&", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("8", "8", "*", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("9", "9", "(", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("0", "0", ")", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("MINUS", "-", "_", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("EQUALS", "=", "+", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSPACE", "Backspace", 2.0, Finger.RIGHT_PINKY, true)
        ));

        // Row 1: QWFPGLUY;[]\
        rows.add(List.of(
                new KeyDef("TAB", "Tab", 1.5, Finger.LEFT_PINKY, true),
                new KeyDef("Q", "q", "Q", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("W", "w", "W", 1.0, Finger.LEFT_RING, false),
                new KeyDef("F", "f", "F", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("P", "p", "P", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("G", "g", "G", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("J", "j", "J", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("L", "l", "L", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("U", "u", "U", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("Y", "y", "Y", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("SEMICOLON", ";", ":", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("LBRACKET", "[", "{", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RBRACKET", "]", "}", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("BACKSLASH", "\\", "|", 1.5, Finger.RIGHT_PINKY, false)
        ));

        // Row 2: ARSTD HNEIO '
        rows.add(List.of(
                new KeyDef("CAPS", "Backspace", 1.8, Finger.LEFT_PINKY, true),
                new KeyDef("A", "a", "A", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("R", "r", "R", 1.0, Finger.LEFT_RING, false),
                new KeyDef("S", "s", "S", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("T", "t", "T", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("D", "d", "D", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("H", "h", "H", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("N", "n", "N", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("E", "e", "E", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("I", "i", "I", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("O", "o", "O", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("QUOTE", "'", "\"", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("ENTER", "Enter", 2.2, Finger.RIGHT_PINKY, true)
        ));

        // Row 3: ZXCVB KM,./
        rows.add(List.of(
                new KeyDef("LSHIFT", "Shift", 2.3, Finger.LEFT_PINKY, true),
                new KeyDef("Z", "z", "Z", 1.0, Finger.LEFT_PINKY, false),
                new KeyDef("X", "x", "X", 1.0, Finger.LEFT_RING, false),
                new KeyDef("C", "c", "C", 1.0, Finger.LEFT_MIDDLE, false),
                new KeyDef("V", "v", "V", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("B", "b", "B", 1.0, Finger.LEFT_INDEX, false),
                new KeyDef("K", "k", "K", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("M", "m", "M", 1.0, Finger.RIGHT_INDEX, false),
                new KeyDef("COMMA", ",", "<", 1.0, Finger.RIGHT_MIDDLE, false),
                new KeyDef("PERIOD", ".", ">", 1.0, Finger.RIGHT_RING, false),
                new KeyDef("SLASH", "/", "?", 1.0, Finger.RIGHT_PINKY, false),
                new KeyDef("RSHIFT", "Shift", 2.7, Finger.RIGHT_PINKY, true)
        ));

        // Row 4: Space row
        rows.add(List.of(
                new KeyDef("LCTRL", "Ctrl", 1.4, Finger.LEFT_PINKY, true),
                new KeyDef("LWIN", "Win", 1.2, Finger.LEFT_PINKY, true),
                new KeyDef("LALT", "Alt", 1.3, Finger.LEFT_RING, true),
                new KeyDef("SPACE", "Space", 6.2, Finger.THUMB, false),
                new KeyDef("RALT", "Alt", 1.3, Finger.RIGHT_RING, true),
                new KeyDef("RWIN", "Fn", 1.2, Finger.RIGHT_PINKY, true),
                new KeyDef("RCTRL", "Ctrl", 1.4, Finger.RIGHT_PINKY, true)
        ));

        return new LayoutInfo("COLEMAK", "Colemak", "Modern ergonomic Colemak layout with optimized home row placement", rows);
    }
}
