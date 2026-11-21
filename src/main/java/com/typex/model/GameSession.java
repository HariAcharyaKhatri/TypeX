package com.typex.model;

/**
 * Represents a completed game session record.
 */
public class GameSession {
    public int id;
    public int userId;
    public String gameType;
    public int score;
    public int levelReached = 1;
    public int wordsCompleted;
    public double accuracy;
    public double wpm;
    public int durationSeconds;
    public int xpEarned;
    public String playedAt;

    public GameSession() {}

    public GameSession(int userId, String gameType, int score, int levelReached,
                       int wordsCompleted, double accuracy, double wpm, int durationSeconds) {
        this.userId = userId;
        this.gameType = gameType;
        this.score = score;
        this.levelReached = levelReached;
        this.wordsCompleted = wordsCompleted;
        this.accuracy = accuracy;
        this.wpm = wpm;
        this.durationSeconds = durationSeconds;
    }
}
