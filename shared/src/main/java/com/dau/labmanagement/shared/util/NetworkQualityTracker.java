package com.dau.labmanagement.shared.util;

import java.util.ArrayDeque;
import java.util.Deque;

public final class NetworkQualityTracker {

    private final int windowSize;
    private final Deque<Boolean> results = new ArrayDeque<>();

    private Double lastRttMillis;

    public NetworkQualityTracker() {
        this(20);
    }

    public NetworkQualityTracker(int windowSize) {

        if (windowSize < 1) {
            throw new IllegalArgumentException(
                    "Window size must be positive");
        }

        this.windowSize = windowSize;
    }

    public synchronized void recordSuccess(
            long sentNanos,
            long receivedNanos) {

        long elapsed = receivedNanos - sentNanos;

        if (elapsed < 0) {
            throw new IllegalArgumentException("Invalid RTT");
        }

        lastRttMillis = elapsed / 1_000_000.0;

        addResult(true);
    }

    public synchronized void recordFailure() {
        addResult(false);
    }

    private void addResult(boolean success) {

        results.addLast(success);

        while (results.size() > windowSize) {
            results.removeFirst();
        }
    }

    public synchronized Double getRttMillis() {
        return lastRttMillis;
    }

    public synchronized Double getPacketLossPercent() {

        if (results.isEmpty()) {
            return null;
        }

        long lost = results.stream()
                .filter(success -> !success)
                .count();

        return lost * 100.0 / results.size();
    }

    public synchronized int getSampleCount() {
        return results.size();
    }

    public synchronized void reset() {
        results.clear();
        lastRttMillis = null;
    }
}
