package com.dau.labmanagement.server.model.service.dashboard;

public final class MachineStatusEvaluator {

    public static final long HEARTBEAT_INTERVAL_MS = 10_000;
    public static final long OFFLINE_TIMEOUT_MS = 30_000;

    public static final double MAX_RTT_MS = 100.0;
    public static final double MAX_PACKET_LOSS = 5.0;

    private MachineStatusEvaluator() {
    }

    public static MachineStatus evaluate(
            long elapsedMillis,
            Double rttMillis,
            Double packetLossPercent) {

        if (elapsedMillis < 0) {
            throw new IllegalArgumentException(
                    "Elapsed time must not be negative");
        }

        if (rttMillis != null &&
                (!Double.isFinite(rttMillis) || rttMillis < 0)) {
            throw new IllegalArgumentException("Invalid RTT");
        }

        if (packetLossPercent != null &&
                (!Double.isFinite(packetLossPercent)
                        || packetLossPercent < 0
                        || packetLossPercent > 100)) {
            throw new IllegalArgumentException(
                    "Invalid packet loss");
        }

        if (elapsedMillis >= OFFLINE_TIMEOUT_MS) {
            return MachineStatus.OFFLINE;
        }

        if (elapsedMillis >= HEARTBEAT_INTERVAL_MS
                || (rttMillis != null && rttMillis > MAX_RTT_MS)
                || (packetLossPercent != null
                    && packetLossPercent > MAX_PACKET_LOSS)) {

            return MachineStatus.UNSTABLE;
        }

        return MachineStatus.ONLINE;
    }
}