package com.typex.util;

/**
 * Pluggable monotonic clock source for timing elapsed durations, intervals,
 * and game state. Allows deterministic testing using simulated clocks.
 */
@FunctionalInterface
public interface TimeSource {

    long nanoTime();

    TimeSource SYSTEM = System::nanoTime;
}
