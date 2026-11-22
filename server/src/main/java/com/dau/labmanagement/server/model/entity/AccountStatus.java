package com.dau.labmanagement.server.model.entity;

import java.util.Locale;

/**
 * Account status, matching the CHECK constraint on app_user.status
 * (active | locked).
 */
public enum AccountStatus {

    /** The account can log in. */
    ACTIVE,

    /** The account is locked by an admin. */
    LOCKED;

    /**
     * Converts a database value to the enum.
     *
     * @param value the value stored in the status column
     * @return the matching status
     * @throws IllegalArgumentException if the value is unknown
     */
    public static AccountStatus fromDb(String value) {
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
