package com.typex.model;

public class TypingResult {
    public int id;
    public int userId;
    public String category;
    public int durationSeconds;
    public double grossWpm;
    public double netWpm;
    public double accuracy;
    public double rawAccuracy;
    public int correctChars;
    public int incorrectChars;
    public int correctedErrors;
    public int uncorrectedErrors;
    public int totalChars;
    public int totalKeystrokes;
    public int rawKeystrokes;
    public int backspaces;
    public double errorPercent;
    public double consistency;
    public String completedAt;
}
