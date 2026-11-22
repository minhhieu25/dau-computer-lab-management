package com.dau.labmanagement.server.model.entity;

import java.util.Locale;

/**
 * Account role, matching the CHECK constraint on app_user.role
 * (admin | teacher | student).
 */
public enum UserRole {

    /** Manages accounts, recording files and retention. */
    ADMIN,

    /** Controls the class from the PC Server. */
    TEACHER,

    /** Logs in on a PC Client. */
    STUDENT;

    /**
     * Converts a database value to the enum.
     *
     * @param value the value stored in the role column
     * @return the matching role
     * @throws IllegalArgumentException if the value is unknown
     */
    public static UserRole fromDb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("role value is null");
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    /**
     * Converts the enum to its database value.
     *
     * @return the lowercase value stored in the role column
     */
    public String toDb() {
        return name().toLowerCase(Locale.ROOT);
    }
}
