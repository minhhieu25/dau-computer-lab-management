package com.dau.labmanagement.server.service.filepush;

import java.util.Objects;
import java.util.Optional;

/** Server-side snapshot of a machine connection and its authenticated student session. */
public record RecipientSession(
        String machineId,
        boolean online,
        boolean studentAuthenticated,
        Optional<String> studentId,
        Optional<String> sessionId,
        boolean sessionIdValid) {

    public RecipientSession {
        machineId = requireText(machineId, "machineId");
        studentId = requireOptionalText(studentId, "studentId");
        sessionId = requireOptionalText(sessionId, "sessionId");
        if (studentAuthenticated && (studentId.isEmpty() || sessionId.isEmpty())) {
            throw new IllegalArgumentException("Authenticated student sessions require studentId and sessionId");
        }
        if (sessionIdValid && sessionId.isEmpty()) {
            throw new IllegalArgumentException("A valid session must have a sessionId");
        }
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        return value.map(item -> requireText(item, fieldName));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}