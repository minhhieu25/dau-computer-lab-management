package com.dau.labmanagement.shared.protocol.message;

public record BeaconMessage(String serverId, int tcpPort, int protocolVersion, long sentAtEpochMillis) {
}
