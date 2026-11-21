package com.dau.labmanagement.server.model.entity;

import java.util.Locale;

/**
 * Submission status, matching the CHECK constraint on submission.status
 * (submitting | submitted | failed).
 */
public enum SubmissionStatus {

    /** Files are still being transferred (BR-05 fail-safe keeps this on disconnect). */
    SUBMITTING,

    /** All files arrived on the server. */
    SUBMITTED,

    /** The transfer failed and must be retried. */
    FAILED;

    /**
     * Converts a database value to the enum.
     *
     * @param value the value stored in the status column
     * @return the matching status
     * @throws IllegalArgumentException if the value is unknown
     */
    public static SubmissionStatus fromDb(String value) {
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
