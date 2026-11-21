package com.dau.labmanagement.shared.auth;

import java.util.Objects;

/** Represents a common authentication result for client and server adapters. */
public record AuthenticationResponse(Status status, Account account) {
    public AuthenticationResponse {
        Objects.requireNonNull(status, "status");
        if ((status == Status.AUTHENTICATED) != (account != null)) {
            throw new IllegalArgumentException("Only authenticated responses contain an account");
        }
    }

    /**
     * Creates a successful authentication response.
     * @param account the authenticated account and session
     * @return the successful response
     */
    public static AuthenticationResponse authenticated(Account account) {
        return new AuthenticationResponse(Status.AUTHENTICATED, Objects.requireNonNull(account, "account"));
    }

    /**
     * Creates a response for credentials that were not accepted.
     * @return the rejected response
     */
    public static AuthenticationResponse invalidCredentials() {
        return new AuthenticationResponse(Status.INVALID_CREDENTIALS, null);
    }

    /**
     * Creates a response for an account that is locked.
     * @return the locked-account response
     */
    public static AuthenticationResponse accountLocked() {
        return new AuthenticationResponse(Status.ACCOUNT_LOCKED, null);
    }

    public enum Status {
        AUTHENTICATED,
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED
    }

    public enum Role {
        ADMIN,
        TEACHER,
        STUDENT
    }

    public record Account(String accountId, String username, String displayName, Role role, String sessionId) {
        public Account {
            accountId = requireText(accountId, "accountId");
            username = requireText(username, "username");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(role, "role");
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
}