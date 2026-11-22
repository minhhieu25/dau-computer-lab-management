package com.dau.labmanagement.server.model.entity;

import java.util.Locale;

/**
 * Submission session status, matching the CHECK constraint on
 * submission_session.status (collecting | completed).
 */
public enum SubmissionSessionStatus {

    /** The teacher is still collecting submissions. */
    COLLECTING,

    /** The collection round has ended. */
    COMPLETED;

    /**
     * Converts a database value to the enum.
     *
     * @param value the value stored in the status column
     * @return the matching status
     * @throws IllegalArgumentException if the value is unknown
     */
    public static SubmissionSessionStatus fromDb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status value is null");
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    /**
     * Converts the enum to its database value.
     *
     * @return the lowercase value stored in the status column
     */
    public String toDb() {
        return name().toLowerCase(Locale.ROOT);
    }
}
