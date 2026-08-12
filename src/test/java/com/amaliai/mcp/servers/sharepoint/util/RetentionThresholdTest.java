package com.amaliai.mcp.servers.sharepoint.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionThresholdTest {

    private static final Instant NOW = Instant.parse("2026-01-31T00:00:00Z");

    @Test
    @DisplayName("a document older than the threshold is stale")
    void marksOlderDocumentStale() {
        RetentionThreshold threshold = RetentionThreshold.ofDays(30);

        assertTrue(threshold.isStale(NOW.minus(Duration.ofDays(31)), NOW));
    }

    @Test
    @DisplayName("a document exactly at the threshold is not yet stale")
    void treatsBoundaryAsFresh() {
        RetentionThreshold threshold = RetentionThreshold.ofDays(30);

        assertFalse(threshold.isStale(NOW.minus(Duration.ofDays(30)), NOW));
    }

    @Test
    @DisplayName("a retention below one day is rejected")
    void rejectsTooShortRetention() {
        assertThrows(IllegalArgumentException.class, () -> RetentionThreshold.ofDays(0));
    }

    @Test
    @DisplayName("age is never negative for a future timestamp")
    void clampsFutureTimestampToZero() {
        RetentionThreshold threshold = RetentionThreshold.ofDays(30);

        assertEquals(Duration.ZERO, threshold.ageOf(NOW.plus(Duration.ofDays(2)), NOW));
    }

    @Test
    @DisplayName("thresholds of the same length are equal")
    void comparesByMaxAge() {
        assertEquals(RetentionThreshold.ofDays(30), RetentionThreshold.ofDays(30));
        assertEquals(RetentionThreshold.ofDays(30).hashCode(), RetentionThreshold.ofDays(30).hashCode());
    }
}
