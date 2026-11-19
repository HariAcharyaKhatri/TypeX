package com.typex;

import com.typex.service.StatisticsService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatisticsServiceTest {
    @Test
    void grossWpm() {
        // 100 chars in 1 minute -> 20 WPM
        assertEquals(20.0, StatisticsService.grossWpm(100, 1.0), 0.01);
    }

    @Test
    void netWpmAccountsForErrors() {
        // (100 - 10) / 5 = 18 WPM
        assertEquals(18.0, StatisticsService.netWpm(100, 10, 1.0), 0.01);
    }

    @Test
    void netWpmCannotBeNegative() {
        // More errors than characters -> 0 WPM, never negative
        assertEquals(0.0, StatisticsService.netWpm(10, 50, 1.0), 0.01);
    }

    @Test
    void accuracyZeroTotal() {
        assertEquals(100.0, StatisticsService.accuracy(0, 0), 0.001);
    }

    @Test
    void accuracyPartial() {
        assertEquals(80.0, StatisticsService.accuracy(40, 50), 0.01);
    }

    @Test
    void zeroMinutesSafe() {
        assertEquals(0.0, StatisticsService.grossWpm(50, 0), 0.001);
        assertEquals(0.0, StatisticsService.netWpm(50, 5, 0), 0.001);
    }

    @Test
    void negativeInputSafe() {
        assertEquals(0.0, StatisticsService.grossWpm(-10, 1.0), 0.001);
        assertEquals(0.0, StatisticsService.grossWpm(50, -1.0), 0.001);
        assertEquals(0.0, StatisticsService.netWpm(50, 5, -1.0), 0.001);
        assertEquals(0.0, StatisticsService.accuracy(-5, 10), 0.001);
    }
}
