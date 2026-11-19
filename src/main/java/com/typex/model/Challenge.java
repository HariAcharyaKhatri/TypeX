package com.typex.model;

public class Challenge {
    public String id;
    public String type; // "DAILY", "WEEKLY"
    public String title;
    public String description;
    public String requirementType; // "TESTS_COUNT", "LESSONS_COUNT", "MIN_WPM", "MIN_ACCURACY", "PRACTICE_MINUTES", "PERFECT_TEST"
    public double targetValue;
    public int xpReward;
    public String startDate;
    public String endDate;

    public Challenge() {}

    public Challenge(String id, String type, String title, String description, String requirementType, double targetValue, int xpReward, String startDate, String endDate) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.requirementType = requirementType;
        this.targetValue = targetValue;
        this.xpReward = xpReward;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
