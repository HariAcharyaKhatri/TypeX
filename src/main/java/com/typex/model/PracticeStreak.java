package com.typex.model;

public class PracticeStreak {
    public int userId;
    public int currentStreak;
    public int longestStreak;
    public String lastPracticeDate;

    public PracticeStreak() {}

    public PracticeStreak(int userId, int currentStreak, int longestStreak, String lastPracticeDate) {
        this.userId = userId;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastPracticeDate = lastPracticeDate;
    }
}
