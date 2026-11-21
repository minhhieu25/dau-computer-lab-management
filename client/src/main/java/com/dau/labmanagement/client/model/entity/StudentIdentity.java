package com.dau.labmanagement.client.model.entity;

import java.util.Objects;

/** Identifies the authenticated student, their client machine, and the auth session. */
public record StudentIdentity(
        String accountId,
        String studentId,
        String displayName,
        String machineId,
        String sessionId) {

    public StudentIdentity {
        accountId = requireText(accountId, "accountId");
        studentId = requireText(studentId, "studentId");
        displayName = requireText(displayName, "displayName");
        machineId = requireText(machineId, "machineId");
        sessionId = requireText(sessionId, "sessionId");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}