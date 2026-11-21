package com.dau.labmanagement.shared.auth;

import java.util.Arrays;
import java.util.Objects;

/** Holds credentials for one authentication attempt and clears its password when closed. */
public final class AuthenticationRequest implements AutoCloseable {
    private final String username;
    private final char[] password;
    private boolean closed;

    /**
     * Creates a credential request with a defensive password copy.
     * @param username the account username
     * @param password the password characters
     */
    public AuthenticationRequest(String username, char[] password) {
        this.username = requireText(username, "username");
        this.password = Objects.requireNonNull(password, "password").clone();
    }

    /** Returns the username without exposing the password. */
    public String username() {
        return username;
    }

    /**
     * Returns a copy of the password while this request remains open.
     * @return a caller-owned password copy
     */
    public synchronized char[] copyPassword() {
        if (closed) {
            throw new IllegalStateException("Authentication request is closed");
        }
        return password.clone();
    }

    /** Erases the stored password copy. */
    @Override
    public synchronized void close() {
        Arrays.fill(password, '\0');
        closed = true;
    }

    /** Returns a representation that never contains the password. */
    @Override
    public String toString() {
        return "AuthenticationRequest[username=" + username + ", password=<redacted>]";
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}