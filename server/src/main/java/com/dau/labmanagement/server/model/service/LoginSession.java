package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An authenticated Server login session (SC_001).
 *
 * <p>The ERD has no table for Server logins, so the session lives in memory in
 * {@link LoginSessionRegistry} for the lifetime of the Server process; see
 * {@code Docs/adr/ADR-001-server-login-session.md}.</p>
 *
 * @param sessionId unique id of this login session
 * @param userId    the authenticated account
 * @param username  login name
 * @param fullName  display name
 * @param role      ADMIN or TEACHER (students log in on the Client, SC_002)
 * @param loginAt   when the session was opened
 */
public record LoginSession(
        UUID sessionId,
        long userId,
        String username,
        String fullName,
        UserRole role,
        OffsetDateTime loginAt) {

    public LoginSession {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(loginAt, "loginAt");
    }

    /** Returns the role name in the UI wording used by Docs/template.md. */
    public String roleLabel() {
        return role == UserRole.ADMIN ? "Admin" : "Giảng viên";
    }
}
