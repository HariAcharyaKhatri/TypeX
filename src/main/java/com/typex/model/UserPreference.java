package com.typex.model;

public class UserPreference {
    public int userId;
    public int dailyGoalMinutes = 15;
    public int dailyGoalTests = 3;
    public int dailyGoalLessons = 1;
    public double dailyGoalAccuracy = 95.0;
    public int preferredDuration = 60;
    public String preferredCategory = "Common English";
    public String keyboardLayout = "US QWERTY";
    public int passageFontSize = 26;
    public boolean soundEnabled = true;
    public int volume = 70;
    public boolean levelUpAlerts = true;
    public boolean achievementAlerts = true;

    public UserPreference() {}
    public UserPreference(int userId) { this.userId = userId; }
}
