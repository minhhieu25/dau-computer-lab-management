package com.dau.labmanagement.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkQualityTrackerTest {

    @Test
    void unknownMetricsBeforeFirstSample() {

        var tracker = new NetworkQualityTracker();

        assertNull(tracker.getRttMillis());
        assertNull(tracker.getPacketLossPercent());
    }

    @Test
    void calculatesRttCorrectly() {

        var tracker = new NetworkQualityTracker();

        tracker.recordSuccess(0, 25_000_000);

        assertEquals(25.0, tracker.getRttMillis());
        assertEquals(0.0, tracker.getPacketLossPercent());
    }

    @Test
    void calculatesFivePercentLoss() {

        var tracker = new NetworkQualityTracker();

        for (int i = 0; i < 19; i++) {
            tracker.recordSuccess(0, 10_000_000);
        }

        tracker.recordFailure();

        assertEquals(5.0, tracker.getPacketLossPercent());
    }

    @Test
    void calculatesTenPercentLoss() {

        var tracker = new NetworkQualityTracker();

        for (int i = 0; i < 18; i++) {
            tracker.recordSuccess(0, 10_000_000);
        }

        tracker.recordFailure();
        tracker.recordFailure();

        assertEquals(10.0, tracker.getPacketLossPercent());
    }

    @Test
    void slidingWindowRemovesOldSamples() {

        var tracker = new NetworkQualityTracker(3);

        tracker.recordFailure();

        for (int i = 0; i < 3; i++) {
            tracker.recordSuccess(0, 10_000_000);
        }

        assertEquals(3, tracker.getSampleCount());
        assertEquals(0.0, tracker.getPacketLossPercent());
    }

    @Test
    void resetClearsMetrics() {

        var tracker = new NetworkQualityTracker();

        tracker.recordSuccess(0, 10_000_000);
        tracker.reset();

        assertNull(tracker.getRttMillis());
        assertNull(tracker.getPacketLossPercent());
        assertEquals(0, tracker.getSampleCount());
    }
}
