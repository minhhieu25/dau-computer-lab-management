package com.dau.labmanagement.shared.protocol.message;

/** Server response for each step of the student LOGIN exchange. */
public record LoginAckMessage(
        LoginStatus status,
        String nonce,
        String salt,
        int iterations,
        Long studentSessionId,
        String studentId,
        String fullName,
        String message) {
}
