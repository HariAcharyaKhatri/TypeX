package com.typex.model;

public class UserXP {
    public int userId;
    public int totalXp;
    public int level;
    public int currentLevelXp;
    public int nextLevelXp;
    public double progressPercent;

    public UserXP() {}

    public UserXP(int userId, int totalXp, int level, int currentLevelXp, int nextLevelXp) {
        this.userId = userId;
        this.totalXp = totalXp;
        this.level = level;
        this.currentLevelXp = currentLevelXp;
        this.nextLevelXp = nextLevelXp;
        int diff = nextLevelXp - currentLevelXp;
        this.progressPercent = diff > 0 ? Math.min(100.0, Math.max(0.0, ((totalXp - currentLevelXp) * 100.0) / diff)) : 0.0;
    }
}
