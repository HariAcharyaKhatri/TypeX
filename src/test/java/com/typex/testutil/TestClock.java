package com.typex.testutil;

import com.typex.util.TimeSource;

/**
 * Deterministic fake time source for unit testing.
 * Supports manual time advancement without relying on wall-clock Thread.sleep.
 */
public class TestClock implements TimeSource {

    private long currentNanos;

    public TestClock(long initialNanos) {
        this.currentNanos = initialNanos;
    }

    public TestClock() {
        this(1_000_000_000L); // 1.0s initial baseline
    }

    @Override
    public long nanoTime() {
        return currentNanos;
    }

    public void advanceNanos(long nanos) {
        this.currentNanos += nanos;
    }

    public void advanceMillis(long millis) {
        this.currentNanos += millis * 1_000_000L;
    }

    public void advanceSeconds(long seconds) {
        this.currentNanos += seconds * 1_000_000_000L;
    }

    public void setNanos(long nanos) {
        this.currentNanos = nanos;
    }
}
