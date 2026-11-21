package com.typex.service;

import java.util.*;

/**
 * Professional Typing Engine for TYPEX.
 *
 * Tracks character-level and word-level accuracy, raw keystrokes, backspace efficiency,
 * corrected and uncorrected errors, and typing rhythm consistency.
 *
 * METRIC FORMULAS:
 * 1. Elapsed Minutes: elapsedSeconds() / 60.0
 * 2. Gross WPM: (typed.length() / 5.0) / elapsedMinutes
 *    - Standard formula: 5 characters equal 1 word. Measures raw typing volume over time.
 * 3. Net WPM: Math.max(0.0, (typed.length() - uncorrectedErrors) / 5.0) / elapsedMinutes
 *    - Standard formula penalizing uncorrected mistakes in the final text.
 * 4. Final Accuracy: (correctChars * 100.0) / (correctChars + uncorrectedErrors)
 *    - Percentage of currently correct characters relative to total characters in typed text.
 * 5. Raw Keystroke Accuracy: Math.max(0.0, (rawKeystrokes - incorrectAttempts - redundantBackspaces) * 100.0 / rawKeystrokes)
 *    - Measures all physical keystroke attempts; errors corrected later still count against raw accuracy.
 * 6. Corrected Errors: Math.max(0, incorrectAttempts - uncorrectedErrors)
 *    - Mistakes that were typed and subsequently erased with backspace.
 * 7. Typing Consistency: 100.0 * Math.max(0.0, 1.0 - (stdDevOfIntervals / (meanInterval + 1e-6)))
 *    - Statistical rhythm analysis based on inter-keystroke interval variability.
 * 8. Backspace Efficiency: (correctBackspaces * 100.0) / Math.max(1, backspaces)
 *    - Measures the percentage of backspaces that erased actual errors vs already correct letters.
 */
public class TypingEngine {
    private final String target;
    private final StringBuilder typed = new StringBuilder();
    private int cursor = 0;

    // Character metrics
    private int correctChars = 0;
    private int uncorrectedErrors = 0;
    private int incorrectAttempts = 0;
    private int totalKeystrokes = 0;
    private int rawKeystrokes = 0;

    // Backspace analytics
    private int backspaces = 0;
    private int correctBackspaces = 0;
    private int redundantBackspaces = 0;

    // Error maps
    private final Map<Character, Integer> currentErrorsByChar = new HashMap<>();
    private final Map<Character, Integer> historicalErrorsByChar = new HashMap<>();

    // Word analytics
    private final String[] targetWords;
    private final int[] wordStartIndices;
    private int currentWordIndex = 0;
    private int completedWords = 0;

    // Timing and rhythm
    private long startNanos = -1;
    private long endNanos = -1;
    private boolean paused = false;
    private long pauseStartNanos = -1;
    private long totalPausedNanos = 0;

    private final List<Long> keystrokeTimestamps = new ArrayList<>();
    private final List<Double> wpmSamples = new ArrayList<>();

    public TypingEngine(String target) {
        this.target = target == null ? "" : target;
        this.targetWords = this.target.isEmpty() ? new String[0] : this.target.split("\\s+");
        this.wordStartIndices = computeWordIndices(this.target, this.targetWords);
    }

    private static int[] computeWordIndices(String text, String[] words) {
        int[] indices = new int[words.length];
        int searchIdx = 0;
        for (int i = 0; i < words.length; i++) {
            int pos = text.indexOf(words[i], searchIdx);
            indices[i] = pos >= 0 ? pos : searchIdx;
            searchIdx = indices[i] + words[i].length();
        }
        return indices;
    }

    // --- State & Getters ---

    public String getTarget() { return target; }
    public String getOriginalText() { return target; }
    public String getTyped() { return typed.toString(); }
    public int getCursor() { return cursor; }
    public int getCorrectChars() { return correctChars; }
    public int getIncorrectChars() { return uncorrectedErrors; }
    public int getUncorrectedErrors() { return uncorrectedErrors; }
    public int getIncorrectAttempts() { return incorrectAttempts; }
    public int getCorrectedErrors() { return Math.max(0, incorrectAttempts - uncorrectedErrors); }
    public int getTotalKeystrokes() { return totalKeystrokes; }
    public int getRawKeystrokes() { return rawKeystrokes; }
    public int getBackspaces() { return backspaces; }
    public int getCorrectBackspaces() { return correctBackspaces; }
    public int getRedundantBackspaces() { return redundantBackspaces; }
    public boolean hasStarted() { return startNanos > 0; }
    public boolean isPaused() { return paused; }

    public Map<Character, Integer> getErrorsByChar() {
        return Collections.unmodifiableMap(currentErrorsByChar);
    }

    public Map<Character, Integer> getHistoricalErrorsByChar() {
        return Collections.unmodifiableMap(historicalErrorsByChar);
    }

    public List<Double> getWpmSamples() {
        return Collections.unmodifiableList(wpmSamples);
    }

    public int getCurrentWordIndex() { return currentWordIndex; }
    public int getCompletedWords() { return completedWords; }
    public int getTotalWords() { return targetWords.length; }

    public double progressPercent() {
        if (target.isEmpty()) return 100.0;
        return Math.min(100.0, (cursor * 100.0) / target.length());
    }

    // --- Lifecycle ---

    public synchronized void start() {
        if (startNanos < 0) {
            startNanos = System.nanoTime();
        }
    }

    public synchronized void stop() {
        if (endNanos < 0) {
            if (paused) resume();
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
        uncorrectedErrors = 0;
        incorrectAttempts = 0;
        totalKeystrokes = 0;
        rawKeystrokes = 0;
        backspaces = 0;
        correctBackspaces = 0;
        redundantBackspaces = 0;
        currentWordIndex = 0;
        completedWords = 0;

        startNanos = -1;
        endNanos = -1;
        paused = false;
        pauseStartNanos = -1;
        totalPausedNanos = 0;

        currentErrorsByChar.clear();
        historicalErrorsByChar.clear();
        keystrokeTimestamps.clear();
        wpmSamples.clear();
    }

    // --- Typing Operations ---

    public synchronized boolean type(char c) {
        rawKeystrokes++;
        if (paused || isComplete()) return false;
        start();

        long now = System.nanoTime();
        keystrokeTimestamps.add(now);

        totalKeystrokes++;
        char expected = target.charAt(cursor);
        boolean ok = (expected == c);

        if (ok) {
            correctChars++;
        } else {
            uncorrectedErrors++;
            incorrectAttempts++;
            currentErrorsByChar.merge(expected, 1, Integer::sum);
            historicalErrorsByChar.merge(expected, 1, Integer::sum);
        }

        typed.append(c);
        cursor++;

        updateWordProgress();
        return ok;
    }

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
            redundantBackspaces++;
        } else {
            uncorrectedErrors--;
            correctBackspaces++;
            // Remove mistake from current active errors map
            Integer cnt = currentErrorsByChar.get(expected);
            if (cnt != null) {
                if (cnt <= 1) currentErrorsByChar.remove(expected);
                else currentErrorsByChar.put(expected, cnt - 1);
            }
        }

        updateWordProgress();
        return true;
    }

    private void updateWordProgress() {
        if (wordStartIndices.length == 0) return;
        int wordIdx = 0;
        for (int i = 0; i < wordStartIndices.length; i++) {
            if (cursor >= wordStartIndices[i]) {
                wordIdx = i;
            } else {
                break;
            }
        }
        currentWordIndex = wordIdx;

        // Check completed words
        int completed = 0;
        for (int i = 0; i < targetWords.length; i++) {
            int start = wordStartIndices[i];
            int end = start + targetWords[i].length();
            if (typed.length() >= end) {
                String typedWord = typed.substring(start, end);
                if (typedWord.equals(targetWords[i])) {
                    completed++;
                }
            }
        }
        completedWords = completed;
    }

    // --- Performance Calculations ---

    public synchronized double elapsedSeconds() {
        if (startNanos < 0) return 0.0;
        long currentEnd;
        if (endNanos > 0) currentEnd = endNanos;
        else if (paused && pauseStartNanos > 0) currentEnd = pauseStartNanos;
        else currentEnd = System.nanoTime();

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
        double net = Math.max(0.0, typed.length() - uncorrectedErrors);
        return (net / 5.0) / minutes;
    }

    public synchronized double accuracy() {
        int total = correctChars + uncorrectedErrors;
        if (total <= 0) return 100.0;
        double acc = (correctChars * 100.0) / total;
        return Math.max(0.0, Math.min(100.0, acc));
    }

    public synchronized double rawAccuracy() {
        if (rawKeystrokes <= 0) return 100.0;
        int successfulStrokes = Math.max(0, rawKeystrokes - incorrectAttempts - redundantBackspaces);
        double acc = (successfulStrokes * 100.0) / rawKeystrokes;
        return Math.max(0.0, Math.min(100.0, acc));
    }

    public synchronized double backspaceEfficiency() {
        if (backspaces <= 0) return 100.0;
        return (correctBackspaces * 100.0) / backspaces;
    }

    public synchronized double typingConsistency() {
        if (keystrokeTimestamps.size() < 3) return 100.0;

        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < keystrokeTimestamps.size(); i++) {
            double ms = (keystrokeTimestamps.get(i) - keystrokeTimestamps.get(i - 1)) / 1_000_000.0;
            // Ignore pauses longer than 2 seconds (e.g. user taking a sip of water)
            if (ms > 0 && ms <= 2000.0) {
                intervals.add(ms);
            }
        }
        if (intervals.size() < 2) return 100.0;

        double mean = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean <= 0.0) return 100.0;

        double sumSq = 0.0;
        for (double d : intervals) {
            sumSq += Math.pow(d - mean, 2);
        }
        double stdDev = Math.sqrt(sumSq / intervals.size());

        // Coefficient of variation metric
        double consistency = 100.0 * Math.max(0.0, 1.0 - (stdDev / (mean + 1e-6)));
        return Math.max(0.0, Math.min(100.0, consistency));
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
