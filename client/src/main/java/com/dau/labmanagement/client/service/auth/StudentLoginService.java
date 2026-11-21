package com.dau.labmanagement.client.service.auth;

import com.dau.labmanagement.client.model.entity.StudentIdentity;
import com.dau.labmanagement.client.model.state.ClientSessionState;
import com.dau.labmanagement.shared.auth.AuthenticationException;
import com.dau.labmanagement.shared.auth.AuthenticationGateway;
import com.dau.labmanagement.shared.auth.AuthenticationRequest;
import com.dau.labmanagement.shared.auth.AuthenticationResponse;
import java.util.Arrays;
import java.util.Objects;

/** Coordinates student login and logout without controlling client network presence. */
public final class StudentLoginService {
    private final AuthenticationGateway authenticationGateway;
    private final ClientSessionState sessionState;
    private final String machineId;

    /**
     * Creates a login service bound to one client machine and session state.
     * @param authenticationGateway the internal authentication adapter
     * @param sessionState the client identity and connection state
     * @param machineId the stable identifier for this client machine
     */
    public StudentLoginService(
            AuthenticationGateway authenticationGateway,
            ClientSessionState sessionState,
            String machineId) {
        this.authenticationGateway = Objects.requireNonNull(authenticationGateway, "authenticationGateway");
        this.sessionState = Objects.requireNonNull(sessionState, "sessionState");
        this.machineId = requireText(machineId, "machineId");
    }

    /**
     * Validates and authenticates a student, clearing the supplied password array afterward.
     * @param studentId the student's MSSV
     * @param password the password characters, cleared before this method returns
     * @return the login outcome
     */
    public synchronized LoginStatus login(String studentId, char[] password) {
        if (password == null) {
            return LoginStatus.MISSING_PASSWORD;
        }

        try {
            if (studentId == null || studentId.isBlank()) {
                return LoginStatus.MISSING_STUDENT_ID;
            }
            if (password.length == 0) {
                return LoginStatus.MISSING_PASSWORD;
            }
            if (sessionState.studentIdentity().isPresent()) {
                return LoginStatus.ALREADY_AUTHENTICATED;
            }

            AuthenticationResponse response;
            try (AuthenticationRequest request = new AuthenticationRequest(studentId.trim(), password)) {
                response = authenticationGateway.authenticate(request);
            } catch (AuthenticationException exception) {
                return LoginStatus.SERVICE_UNAVAILABLE;
            }

            if (response == null) {
                return LoginStatus.SERVICE_UNAVAILABLE;
            }
            if (response.status() == AuthenticationResponse.Status.INVALID_CREDENTIALS) {
                return LoginStatus.INVALID_CREDENTIALS;
            }
            if (response.status() == AuthenticationResponse.Status.ACCOUNT_LOCKED) {
                return LoginStatus.ACCOUNT_LOCKED;
            }

            AuthenticationResponse.Account account = response.account();
            if (account.role() != AuthenticationResponse.Role.STUDENT) {
                return LoginStatus.WRONG_ROLE;
            }

            StudentIdentity identity = new StudentIdentity(
                    account.accountId(),
                    account.username(),
                    account.displayName(),
                    machineId,
                    account.sessionId());
            return sessionState.attachStudentIdentity(identity)
                    ? LoginStatus.AUTHENTICATED
                    : LoginStatus.ALREADY_AUTHENTICATED;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Ends the remote session and always clears the local identity.
     * @return the local and remote logout outcome
     */
    public synchronized LogoutStatus logout() {
        StudentIdentity identity = sessionState.studentIdentity().orElse(null);
        if (identity == null) {
            return LogoutStatus.NOT_AUTHENTICATED;
        }

        try {
            authenticationGateway.logout(identity.sessionId());
            return LogoutStatus.LOGGED_OUT;
        } catch (AuthenticationException exception) {
            return LogoutStatus.LOGGED_OUT_REMOTELY_UNAVAILABLE;
        } finally {
            sessionState.clearStudentIdentity();
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public enum LoginStatus {
        AUTHENTICATED,
        MISSING_STUDENT_ID,
        MISSING_PASSWORD,
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        WRONG_ROLE,
        ALREADY_AUTHENTICATED,
        SERVICE_UNAVAILABLE
    }

    public enum LogoutStatus {
        LOGGED_OUT,
        LOGGED_OUT_REMOTELY_UNAVAILABLE,
        NOT_AUTHENTICATED
    }
}