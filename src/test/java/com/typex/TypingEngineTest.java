package com.typex;

import com.typex.service.TypingEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TypingEngineTest {
    @Test
    void basicTyping() {
        TypingEngine e = new TypingEngine("hello");
        assertTrue(e.type('h'));
        assertTrue(e.type('e'));
        assertTrue(e.type('l'));
        assertTrue(e.type('l'));
        assertTrue(e.type('o'));
        assertTrue(e.isComplete());
        assertEquals(5, e.getCorrectChars());
        assertEquals(0, e.getIncorrectChars());
        assertEquals(5, e.getTotalKeystrokes());
        assertEquals(5, e.getRawKeystrokes());
        assertEquals(100.0, e.accuracy(), 0.001);
    }

    @Test
    void incorrectCharsTracked() {
        TypingEngine e = new TypingEngine("abc");
        assertFalse(e.type('x'));
        assertFalse(e.type('y'));
        assertFalse(e.type('z'));
        assertEquals(0, e.getCorrectChars());
        assertEquals(3, e.getIncorrectChars());
        assertEquals(0.0, e.accuracy(), 0.001);
        assertEquals(100.0, e.errorPercent(), 0.001);
        assertEquals(1, e.getErrorsByChar().get('a'));
        assertEquals(1, e.getErrorsByChar().get('b'));
        assertEquals(1, e.getErrorsByChar().get('c'));
    }

    @Test
    void backspaceRevertsAndUpdatesErrors() {
        TypingEngine e = new TypingEngine("abc");
        e.type('x'); // wrong: expected 'a'
        assertEquals(1, e.getIncorrectChars());
        assertEquals(1, e.getErrorsByChar().get('a'));
        assertEquals(1, e.getRawKeystrokes());

        assertTrue(e.backspace());
        assertEquals(0, e.getIncorrectChars());
        assertFalse(e.getErrorsByChar().containsKey('a'), "Deleted mistake should be removed from errorsByChar");
        assertEquals(0, e.getCursor());
        assertEquals(1, e.getBackspaces());
        assertEquals(2, e.getRawKeystrokes());

        // Now type correct
        assertTrue(e.type('a'));
        assertEquals(1, e.getCorrectChars());
        assertEquals(0, e.getIncorrectChars());
        assertEquals(100.0, e.accuracy(), 0.001);
    }

    @Test
    void backspaceOnEmpty() {
        TypingEngine e = new TypingEngine("abc");
        assertFalse(e.backspace());
        assertEquals(0, e.getBackspaces());
        assertEquals(1, e.getRawKeystrokes());
    }

    @Test
    void accuracyCalculation() {
        TypingEngine e = new TypingEngine("abcd");
        e.type('a'); // ok
        e.type('b'); // ok
        e.type('x'); // err
        e.type('y'); // err
        assertEquals(50.0, e.accuracy(), 0.01);
        assertEquals(50.0, e.errorPercent(), 0.01);
    }

    @Test
    void emptyTextAccuracy() {
        TypingEngine e = new TypingEngine("");
        assertEquals(100.0, e.accuracy(), 0.001);
        assertEquals(0.0, e.errorPercent(), 0.001);
        assertTrue(e.isComplete());
    }

    @Test
    void zeroElapsedReturnsZeroWpm() {
        TypingEngine e = new TypingEngine("hello world");
        assertEquals(0.0, e.grossWpm(), 0.001);
        assertEquals(0.0, e.netWpm(), 0.001);
    }

    @Test
    void pauseAndResumeMechanics() {
        TypingEngine e = new TypingEngine("typing test");
        e.type('t');
        e.pause();
        assertTrue(e.isPaused());

        // While paused, typing is rejected
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
        assertEquals(0, e.getIncorrectChars());
        assertEquals(0, e.getRawKeystrokes());
        assertEquals(0, e.getBackspaces());
        assertTrue(e.getErrorsByChar().isEmpty());
    }
}
