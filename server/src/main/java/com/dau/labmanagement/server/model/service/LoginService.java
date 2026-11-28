package com.dau.labmanagement.server.model.service;

/**
 * Authenticates a Server (teacher/admin) account for SC_001.
 *
 * <p>Implementations must reject invalid accounts instead of throwing:
 * the form always receives a {@link LoginResult}.</p>
 */
@FunctionalInterface
public interface LoginService {

    /**
     * Verifies the credentials and opens a session on success.
     *
     * @param username the entered login name
     * @param password the entered password; implementations must not keep it
     * @return the outcome, never null
     */
    LoginResult login(String username, char[] password);

    /**
     * Returns a service used when the database is not configured; every attempt
     * is rejected with {@link LoginFailureReason#SYSTEM_UNAVAILABLE}.
     *
     * @return a service that never authenticates
     */
    static LoginService unavailable() {
        return (username, password) -> LoginResult.rejected(LoginFailureReason.SYSTEM_UNAVAILABLE);
    }
}
