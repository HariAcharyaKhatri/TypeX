package com.typex.model;

public class DailyGoal {
    public int id;
    public int userId;
    public String date;
    public int targetMinutes = 15;
    public int targetTests = 3;
    public int targetLessons = 1;
    public double targetAccuracy = 95.0;
    public int completedSeconds;
    public int completedTests;
    public int completedLessons;
    public boolean achieved;
    public boolean rewardClaimed;

    public double getProgressPercentage() {
        double minutesProg = targetMinutes > 0 ? Math.min(1.0, (completedSeconds / 60.0) / targetMinutes) : 1.0;
        double testsProg = targetTests > 0 ? Math.min(1.0, (double) completedTests / targetTests) : 1.0;
        double lessonsProg = targetLessons > 0 ? Math.min(1.0, (double) completedLessons / targetLessons) : 1.0;
        return ((minutesProg + testsProg + lessonsProg) / 3.0) * 100.0;
    }
}
