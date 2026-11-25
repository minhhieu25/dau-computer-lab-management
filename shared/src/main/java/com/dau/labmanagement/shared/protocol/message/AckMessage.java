package com.dau.labmanagement.shared.protocol.message;

public record AckMessage(String clientId, long sequence, long serverReceivedAtEpochMillis) {
}
