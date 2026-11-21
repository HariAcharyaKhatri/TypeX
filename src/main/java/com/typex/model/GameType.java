package com.typex.model;

/**
 * Enumeration of all 8 playable games in TYPEX.
 */
public enum GameType {
    WORD_RUSH("word_rush", "Word Rush", "High-Speed Word Streak",
            "Type rapid-fire words before time runs out. Chain consecutive correct words to climb the combo multiplier!",
            "rocket", "Normal"),

    FALLING_WORDS("falling_words", "Falling Words", "Sky-Fall Matrix",
            "Words cascade down the screen at increasing speeds. Type them before they hit the ground line!",
            "flame", "Normal"),

    TYPING_SURVIVAL("typing_survival", "Typing Survival", "Beat the Clock",
            "Vitality continuously drains. Correct words grant bonus seconds, while errors penalize remaining time!",
            "heart", "Hard"),

    LETTER_INVADERS("letter_invaders", "Letter Invaders", "Arcade Space Defense",
            "Alien invaders descend with letter beacons. Strike matching keys to fire defense lasers!",
            "alien", "Normal"),

    SENTENCE_SPRINT("sentence_sprint", "Sentence Sprint", "Drag-Race Speed Sprint",
            "Sprint through sentences at top speed. Race against a ghost pace marker with zero hesitation!",
            "speed", "Easy"),

    KEY_MASTER("key_master", "Key Master", "Layout Reflex Trainer",
            "Pulsing keys across your active keyboard layout test your pure motor reflexes and finger placement.",
            "keyboard", "Intermediate"),

    CODE_RUNNER("code_runner", "Code Runner", "Syntax & Programmer Sprint",
            "Real-world code snippets across Java, Python, JavaScript, HTML, and SQL. Test your speed on brackets and symbols!",
            "code", "Expert"),

    NUMBER_NINJA("number_ninja", "Number Ninja", "Numeric & Symbol Slice",
            "Rapid-fire slicing through numbers, financial figures ($1,250.00), percentages, math equations, and dates!",
            "hash", "Hard");

    public final String id;
    public final String displayName;
    public final String tagline;
    public final String description;
    public final String iconKey;
    public final String difficulty;

    GameType(String id, String displayName, String tagline, String description, String iconKey, String difficulty) {
        this.id = id;
        this.displayName = displayName;
        this.tagline = tagline;
        this.description = description;
        this.iconKey = iconKey;
        this.difficulty = difficulty;
    }

    public static GameType fromId(String id) {
        for (GameType gt : values()) {
            if (gt.id.equalsIgnoreCase(id) || gt.name().equalsIgnoreCase(id)) {
                return gt;
            }
        }
        return WORD_RUSH;
    }
}
