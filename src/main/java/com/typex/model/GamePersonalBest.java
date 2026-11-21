package com.typex.model;

/**
 * Represents a user's high score and personal best record for a game.
 */
public class GamePersonalBest {
    public int id;
    public int userId;
    public String gameType;
    public int highScore;
    public int highestLevel = 1;
    public double bestWpm;
    public double bestAccuracy;
    public String achievedAt;

    public GamePersonalBest() {}

    public GamePersonalBest(int userId, String gameType, int highScore, int highestLevel,
                            double bestWpm, double bestAccuracy) {
        this.userId = userId;
        this.gameType = gameType;
        this.highScore = highScore;
        this.highestLevel = highestLevel;
        this.bestWpm = bestWpm;
        this.bestAccuracy = bestAccuracy;
    }
}
