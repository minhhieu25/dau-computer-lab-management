package com.dau.labmanagement.server.model.service;

import java.util.Objects;

/**
 * Outcome of a Server login attempt: either an open {@link LoginSession} or a
 * {@link LoginFailureReason}.
 */
public record LoginResult(LoginSession session, LoginFailureReason failure) {

    public LoginResult {
        if ((session == null) == (failure == null)) {
            throw new IllegalArgumentException("Exactly one of session or failure is required");
        }
    }

    /** Creates a successful result holding the new session. */
    public static LoginResult success(LoginSession session) {
        return new LoginResult(Objects.requireNonNull(session, "session"), null);
    }

    /** Creates a rejected result. */
    public static LoginResult rejected(LoginFailureReason reason) {
        return new LoginResult(null, Objects.requireNonNull(reason, "reason"));
    }

    public boolean isSuccess() {
        return session != null;
    }

    /** Returns the failure message, or an empty string on success. */
    public String failureMessage() {
        return failure == null ? "" : failure.message();
    }
}
