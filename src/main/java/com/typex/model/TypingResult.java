package com.typex.model;

public class TypingResult {
    public int id, userId, durationSeconds, correctChars, incorrectChars,
               totalChars, totalKeystrokes, backspaces;
    public String category;
    public double grossWpm, netWpm, accuracy, errorPercent;
    public String completedAt;
}
