package com.dau.labmanagement.shared.protocol.message;

public record HeartbeatMessage(String clientId, long sequence, long sentAtEpochMillis) {
}
