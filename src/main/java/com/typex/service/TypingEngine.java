package com.typex.service;

import java.util.*;

/**
 * Core typing engine. Tracks text, cursor, correctness, WPM, accuracy.
 * Pure Java, no Swing/DB coupling - fully unit-testable.
 */
public class TypingEngine {
    private final String target;
    private final StringBuilder typed = new StringBuilder();
    private int cursor = 0;
    private int correctChars = 0;
    private int incorrectChars = 0;
    private int totalKeystrokes = 0;
    private int rawKeystrokes = 0;
    private int backspaces = 0;
    private long startNanos = -1;
    private long endNanos = -1;
    private boolean paused = false;
    private long pauseStartNanos = -1;
    private long totalPausedNanos = 0;
    private final Map<Character, Integer> errorsByChar = new HashMap<>();
    private final List<Double> wpmSamples = new ArrayList<>();

    public TypingEngine(String target) {
        this.target = target == null ? "" : target;
    }

    public String getTarget() { return target; }
    public String getTyped() { return typed.toString(); }
    public int getCursor() { return cursor; }
    public int getCorrectChars() { return correctChars; }
    public int getIncorrectChars() { return incorrectChars; }
    public int getTotalKeystrokes() { return totalKeystrokes; }
    public int getRawKeystrokes() { return rawKeystrokes; }
    public int getBackspaces() { return backspaces; }
    public Map<Character, Integer> getErrorsByChar() { return Collections.unmodifiableMap(errorsByChar); }
    public List<Double> getWpmSamples() { return Collections.unmodifiableList(wpmSamples); }

    public boolean hasStarted() { return startNanos > 0; }
    public boolean isPaused() { return paused; }

    public synchronized void start() {
        if (startNanos < 0) {
            startNanos = System.nanoTime();
        }
    }

    public synchronized void stop() {
        if (endNanos < 0) {
            if (paused) {
                resume();
            }
            endNanos = System.nanoTime();
        }
    }

    public synchronized void pause() {
        if (!paused && hasStarted() && endNanos < 0) {
            paused = true;
            pauseStartNanos = System.nanoTime();
        }
    }

    public synchronized void resume() {
        if (paused) {
            paused = false;
            if (pauseStartNanos > 0) {
                totalPausedNanos += (System.nanoTime() - pauseStartNanos);
                pauseStartNanos = -1;
            }
        }
    }

    public synchronized void reset() {
        typed.setLength(0);
        cursor = 0;
        correctChars = 0;
        incorrectChars = 0;
        totalKeystrokes = 0;
        rawKeystrokes = 0;
        backspaces = 0;
        startNanos = -1;
        endNanos = -1;
        paused = false;
        pauseStartNanos = -1;
        totalPausedNanos = 0;
        errorsByChar.clear();
        wpmSamples.clear();
    }

    /** Returns true if keystroke was correct. */
    public synchronized boolean type(char c) {
        rawKeystrokes++;
        if (paused || isComplete()) return false;
        start();

        totalKeystrokes++;
        char expected = target.charAt(cursor);
        boolean ok = (expected == c);
        if (ok) {
            correctChars++;
        } else {
            incorrectChars++;
            errorsByChar.merge(expected, 1, Integer::sum);
        }
        typed.append(c);
        cursor++;
        return ok;
    }

    /** Returns true if a character was actually removed. */
    public synchronized boolean backspace() {
        rawKeystrokes++;
        if (paused || cursor == 0 || typed.length() == 0) return false;
        start();

        backspaces++;
        cursor--;
        char removed = typed.charAt(typed.length() - 1);
        typed.deleteCharAt(typed.length() - 1);
        char expected = target.charAt(cursor);

        if (removed == expected) {
            correctChars--;
        } else {
            incorrectChars--;
            // Remove deleted mistakes from current error statistics
            Integer count = errorsByChar.get(expected);
            if (count != null) {
                if (count <= 1) {
                    errorsByChar.remove(expected);
                } else {
                    errorsByChar.put(expected, count - 1);
                }
            }
        }
        return true;
    }

    public synchronized double elapsedSeconds() {
        if (startNanos < 0) return 0.0;
        long currentEnd;
        if (endNanos > 0) {
            currentEnd = endNanos;
        } else if (paused && pauseStartNanos > 0) {
            currentEnd = pauseStartNanos;
        } else {
            currentEnd = System.nanoTime();
        }

        long activeNanos = (currentEnd - startNanos) - totalPausedNanos;
        if (activeNanos < 0) activeNanos = 0;
        return activeNanos / 1_000_000_000.0;
    }

    public synchronized double grossWpm() {
        double minutes = elapsedSeconds() / 60.0;
        if (minutes <= 0.0) return 0.0;
        return (typed.length() / 5.0) / minutes;
    }

    public synchronized double netWpm() {
        double minutes = elapsedSeconds() / 60.0;
        if (minutes <= 0.0) return 0.0;
        double net = Math.max(0.0, typed.length() - incorrectChars);
        return (net / 5.0) / minutes;
    }

    public synchronized double accuracy() {
        int total = correctChars + incorrectChars;
        if (total <= 0) return 100.0;
        double acc = (correctChars * 100.0) / total;
        return Math.max(0.0, Math.min(100.0, acc));
    }

    public synchronized double errorPercent() {
        return Math.max(0.0, 100.0 - accuracy());
    }

    public synchronized boolean isComplete() {
        return cursor >= target.length();
    }

    public synchronized void sampleWpm() {
        wpmSamples.add(netWpm());
    }
}
