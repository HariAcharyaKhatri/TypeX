package com.typex.model;

public class UserChallenge {
    public int id;
    public int userId;
    public String challengeId;
    public double currentProgress;
    public boolean completed;
    public boolean rewardClaimed;
    public String completedAt;
    public Challenge challenge;

    public double getProgressPercentage() {
        if (completed) return 100.0;
        if (challenge == null || challenge.targetValue <= 0) return 0.0;
        return Math.min(100.0, Math.max(0.0, (currentProgress * 100.0) / challenge.targetValue));
    }
}
