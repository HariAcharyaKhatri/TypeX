package com.typex;

import com.typex.service.TypingEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TypingEngineTest {

    @Test
    void basicTypingAndProgress() {
        TypingEngine e = new TypingEngine("hello world");
        assertTrue(e.type('h'));
        assertTrue(e.type('e'));
        assertTrue(e.type('l'));
        assertTrue(e.type('l'));
        assertTrue(e.type('o'));
        assertEquals(5, e.getCorrectChars());
        assertEquals(0, e.getUncorrectedErrors());
        assertEquals(5, e.getTotalKeystrokes());
        assertEquals(5, e.getRawKeystrokes());
        assertEquals(100.0, e.accuracy(), 0.001);
        assertEquals(100.0, e.rawAccuracy(), 0.001);
        assertTrue(e.progressPercent() > 0.0);
    }

    @Test
    void wordLevelAccuracyAndCompletion() {
        TypingEngine e = new TypingEngine("the quick brown fox");
        assertEquals(4, e.getTotalWords());
        assertEquals(0, e.getCompletedWords());
        assertEquals(0, e.getCurrentWordIndex());

        // Type "the "
        e.type('t'); e.type('h'); e.type('e'); e.type(' ');
        assertEquals(1, e.getCompletedWords());
        assertEquals(1, e.getCurrentWordIndex());

        // Type "quick "
        e.type('q'); e.type('u'); e.type('i'); e.type('c'); e.type('k'); e.type(' ');
        assertEquals(2, e.getCompletedWords());
        assertEquals(2, e.getCurrentWordIndex());
    }

    @Test
    void correctedVsUncorrectedMistakes() {
        TypingEngine e = new TypingEngine("abc");
        e.type('x'); // Mistake: expected 'a'
        assertEquals(1, e.getUncorrectedErrors());
        assertEquals(1, e.getIncorrectAttempts());
        assertEquals(0, e.getCorrectedErrors());
        assertEquals(1, e.getHistoricalErrorsByChar().get('a'));

        // Backspace to fix mistake
        assertTrue(e.backspace());
        assertEquals(0, e.getUncorrectedErrors(), "Uncorrected errors should be 0 after backspace");
        assertEquals(1, e.getIncorrectAttempts(), "Incorrect attempts must remain 1");
        assertEquals(1, e.getCorrectedErrors(), "Corrected errors should be 1");
        assertEquals(1, e.getCorrectBackspaces());
        assertEquals(0, e.getRedundantBackspaces());

        // Historical errors map MUST still remember the mistake!
        assertEquals(1, e.getHistoricalErrorsByChar().get('a'), "Historical error analytics must retain the error");
        // But current active errors map must be cleared
        assertFalse(e.getErrorsByChar().containsKey('a'));

        // Type correct character
        assertTrue(e.type('a'));
        assertEquals(1, e.getCorrectChars());
        assertEquals(100.0, e.accuracy(), 0.001, "Final accuracy should be 100% since text is currently correct");
        assertTrue(e.rawAccuracy() < 100.0, "Raw keystroke accuracy should penalize the mistyped keystroke");
    }

    @Test
    void redundantBackspacesTracked() {
        TypingEngine e = new TypingEngine("hello");
        e.type('h'); // correct
        assertEquals(1, e.getCorrectChars());

        e.backspace(); // erased a correct char
        assertEquals(0, e.getCorrectChars());
        assertEquals(1, e.getBackspaces());
        assertEquals(0, e.getCorrectBackspaces());
        assertEquals(1, e.getRedundantBackspaces());
        assertEquals(0.0, e.backspaceEfficiency(), 0.01);
    }

    @Test
    void accuracyAndWpmCalculations() {
        TypingEngine e = new TypingEngine("abcd");
        e.type('a'); // ok
        e.type('b'); // ok
        e.type('x'); // wrong
        e.type('y'); // wrong

        assertEquals(50.0, e.accuracy(), 0.01);
        assertEquals(50.0, e.errorPercent(), 0.01);
        assertEquals(2, e.getUncorrectedErrors());
        assertEquals(2, e.getCorrectChars());
    }

    @Test
    void emptyTargetHandling() {
        TypingEngine e = new TypingEngine("");
        assertEquals(100.0, e.accuracy(), 0.001);
        assertEquals(100.0, e.rawAccuracy(), 0.001);
        assertTrue(e.isComplete());
        assertEquals(100.0, e.progressPercent(), 0.001);
        assertEquals(0, e.getTotalWords());
    }

    @Test
    void typingConsistencyCalculation() throws Exception {
        TypingEngine e = new TypingEngine("abcdef");
        // Before 3 keystrokes, default consistency is 100%
        assertEquals(100.0, e.typingConsistency(), 0.001);

        for (char c : "abcdef".toCharArray()) {
            e.type(c);
            Thread.sleep(10);
        }
        double consistency = e.typingConsistency();
        assertTrue(consistency >= 0.0 && consistency <= 100.0, "Consistency must be bounded [0, 100]");
    }

    @Test
    void pauseAndResumeMechanics() {
        TypingEngine e = new TypingEngine("typing test");
        e.type('t');
        e.pause();
        assertTrue(e.isPaused());

        // Typing ignored while paused
        assertFalse(e.type('y'));
        assertEquals(1, e.getCursor());

        e.resume();
        assertFalse(e.isPaused());
        assertTrue(e.type('y'));
        assertEquals(2, e.getCursor());
    }

    @Test
    void resetMechanics() {
        TypingEngine e = new TypingEngine("quick");
        e.type('q');
        e.type('x');
        e.reset();

        assertEquals(0, e.getCursor());
        assertEquals(0, e.getCorrectChars());
        assertEquals(0, e.getUncorrectedErrors());
        assertEquals(0, e.getIncorrectAttempts());
        assertEquals(0, e.getRawKeystrokes());
        assertEquals(0, e.getBackspaces());
        assertEquals(0, e.getCompletedWords());
        assertTrue(e.getErrorsByChar().isEmpty());
        assertTrue(e.getHistoricalErrorsByChar().isEmpty());
    }
}
