package com.typex.model;

public class PracticeRecommendation {
    public enum Type { WEAK_KEYS, NEXT_LESSON, ACCURACY_BOOSTER, SPEED_SPRINT }

    public Type type;
    public String title;
    public String description;
    public String reason;
    public String actionPage; // "trainer", "learn", "test"
    public String buttonText;
    public Object extraData;

    public PracticeRecommendation(Type type, String title, String description, String reason, String actionPage, String buttonText) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.reason = reason;
        this.actionPage = actionPage;
        this.buttonText = buttonText;
    }
}
