package com.dau.labmanagement.shared.protocol.message;

/**
 * Student login request. The first request contains only the student ID;
 * the second contains the server challenge and its password proof.
 */
public record LoginMessage(String studentId, String nonce, String proof) {
}
