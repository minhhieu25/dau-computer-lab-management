package com.dau.labmanagement.server.model.service.dashboard;

import java.time.Instant;

public record MachineSnapshot(
        String clientId,
        String hostname,
        String ipAddress,
        String seatLabel,
        String studentId,
        String studentName,
        Instant lastHeartbeat,
        Double rttMillis,
        Double packetLossPercent,
        MachineStatus status
) {

    public String displayStudent() {
        if (studentName == null || studentName.isBlank()) {
            return "Chưa đăng nhập";
        }

        return studentName;
    }

    public String displayRtt() {
        return rttMillis == null
                ? "—"
                : String.format("%.1f ms", rttMillis);
    }

    public String displayPacketLoss() {
        return packetLossPercent == null
                ? "—"
                : String.format("%.1f%%", packetLossPercent);
    }
}