package com.dau.labmanagement.shared.protocol.message;

public record HeartbeatMessage(
        String clientId,
        long sequence,
        long sentAtEpochMillis,
        Double rttMillis,
        Double packetLossPercent) {

    public HeartbeatMessage(
            String clientId,
            long sequence,
            long sentAtEpochMillis) {

        this(clientId, sequence, sentAtEpochMillis, null, null);
    }
}