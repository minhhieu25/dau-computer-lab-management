package com.dau.labmanagement.server.model.entity;

import java.util.Locale;

/**
 * Recording file status, matching the CHECK constraint on
 * recording_file.status (recording | stored | deleted).
 */
public enum RecordingFileStatus {

    /** The recording is being written (S4). */
    RECORDING,

    /** The recording is complete and replayable. */
    STORED,

    /** The recording was deleted by an admin or the retention job. */
    DELETED;

    /**
     * Converts a database value to the enum.
     *
     * @param value the value stored in the status column
     * @return the matching status
     * @throws IllegalArgumentException if the value is unknown
     */
    public static RecordingFileStatus fromDb(String value) {
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
