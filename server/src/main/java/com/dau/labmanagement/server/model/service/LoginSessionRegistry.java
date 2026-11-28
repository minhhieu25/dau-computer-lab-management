package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the Server login sessions of the running process (SC_001).
 *
 * <p>Thread-safe: connections and JavaFX callbacks may open or close sessions
 * concurrently. Sessions are never persisted, see
 * {@code Docs/adr/ADR-001-server-login-session.md}.</p>
 */
public final class LoginSessionRegistry {

    private final Map<UUID, LoginSession> sessions = new ConcurrentHashMap<>();

    /**
     * Opens a new session for an authenticated account.
     *
     * @param userId   the account id
     * @param username the login name
     * @param fullName the display name
     * @param role     ADMIN or TEACHER
     * @return the opened session
     */
    public LoginSession open(long userId, String username, String fullName, UserRole role) {
        LoginSession session = new LoginSession(
                UUID.randomUUID(), userId, username, fullName, role, OffsetDateTime.now());
        sessions.put(session.sessionId(), session);
        return session;
    }

    /** Finds an active session by id. */
    public Optional<LoginSession> find(UUID sessionId) {
        return sessionId == null ? Optional.empty() : Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Closes a session (logout).
     *
     * @param sessionId the session to close
     * @return true when the session existed
     */
    public boolean close(UUID sessionId) {
        return sessionId != null && sessions.remove(sessionId) != null;
    }

    /** Returns a snapshot of the active sessions. */
    public List<LoginSession> activeSessions() {
        return List.copyOf(sessions.values());
    }

    /** Number of active sessions. */
    public int activeCount() {
        return sessions.size();
    }

    /** Closes every session, used when the Server shuts down. */
    public void closeAll() {
        sessions.clear();
    }
}
