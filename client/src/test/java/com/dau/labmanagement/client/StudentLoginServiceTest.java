package com.dau.labmanagement.client;

import com.dau.labmanagement.client.model.entity.StudentIdentity;
import com.dau.labmanagement.client.model.state.ClientSessionState;
import com.dau.labmanagement.client.service.auth.StudentLoginService;
import com.dau.labmanagement.client.viewmodel.StudentLoginViewModel;
import com.dau.labmanagement.shared.auth.AuthenticationException;
import com.dau.labmanagement.shared.auth.AuthenticationGateway;
import com.dau.labmanagement.shared.auth.AuthenticationRequest;
import com.dau.labmanagement.shared.auth.AuthenticationResponse;
import java.util.Arrays;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class StudentLoginServiceTest {
    private static final String STUDENT_ID = "20260001";
    private static final String MACHINE_ID = "lab-pc-17";
    private static final AuthenticationResponse.Account STUDENT_ACCOUNT = new AuthenticationResponse.Account(
            "account-1", STUDENT_ID, "Nguyen Van An", AuthenticationResponse.Role.STUDENT, "session-1");

    private RecordingAuthenticationGateway gateway;
    private ClientSessionState sessionState;
    private StudentLoginService loginService;

    @BeforeEach
    void setUp() {
        gateway = new RecordingAuthenticationGateway();
        sessionState = new ClientSessionState();
        loginService = new StudentLoginService(gateway, sessionState, MACHINE_ID);
    }

    @AfterEach
    void clearCapturedPassword() {
        if (gateway.capturedPassword != null) {
            Arrays.fill(gateway.capturedPassword, '\0');
        }
    }

    @Test
    void rejectsBlankStudentIdAndEmptyPasswordWithoutCallingGateway() {
        char[] password = {'s', 'e', 'c', 'r', 'e', 't'};

        assertEquals(StudentLoginService.LoginStatus.MISSING_STUDENT_ID,
                loginService.login("  ", password));
        assertArrayEquals(new char[password.length], password);
        assertEquals(StudentLoginService.LoginStatus.MISSING_PASSWORD,
                loginService.login(STUDENT_ID, new char[0]));
        assertEquals(0, gateway.authenticationCalls);
    }

    @Test
    void authenticatesStudentAndAssociatesIdentityWithMachineAndServerSession() {
        gateway.response = AuthenticationResponse.authenticated(STUDENT_ACCOUNT);
        sessionState.markOnline();
        char[] password = "secret".toCharArray();

        assertEquals(StudentLoginService.LoginStatus.AUTHENTICATED,
                loginService.login(STUDENT_ID, password));

        StudentIdentity identity = sessionState.studentIdentity().orElseThrow();
        assertEquals(STUDENT_ID, identity.studentId());
        assertEquals("account-1", identity.accountId());
        assertEquals("Nguyen Van An", identity.displayName());
        assertEquals(MACHINE_ID, identity.machineId());
        assertEquals("session-1", identity.sessionId());
        assertEquals(STUDENT_ID, gateway.receivedUsername);
        assertArrayEquals("secret".toCharArray(), gateway.capturedPassword);
        assertArrayEquals(new char[password.length], password);
        assertTrue(sessionState.isOnline());
    }

    @Test
    void reportsInvalidCredentialsWithoutAttachingIdentity() {
        gateway.response = AuthenticationResponse.invalidCredentials();
        sessionState.markOnline();

        assertEquals(StudentLoginService.LoginStatus.INVALID_CREDENTIALS,
                loginService.login(STUDENT_ID, "bad-password".toCharArray()));

        assertTrue(sessionState.studentIdentity().isEmpty());
        assertTrue(sessionState.isOnline());
    }

    @Test
    void reportsGatewayFailureWithoutCreatingIdentity() {
        gateway.authenticationException = new AuthenticationException("connection lost");
        char[] password = "secret".toCharArray();

        assertEquals(StudentLoginService.LoginStatus.SERVICE_UNAVAILABLE,
                loginService.login(STUDENT_ID, password));

        assertArrayEquals(new char[password.length], password);
        assertTrue(sessionState.studentIdentity().isEmpty());
    }

    @Test
    void reportsGatewayTimeoutWithoutCreatingIdentityOrHanging() {
        gateway.authenticationException = new AuthenticationException("authentication timed out");
        gateway.failureDelayMillis = 25;

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertEquals(
                StudentLoginService.LoginStatus.SERVICE_UNAVAILABLE,
                loginService.login(STUDENT_ID, "secret".toCharArray())));

        assertTrue(sessionState.studentIdentity().isEmpty());
    }

    @Test
    void unexpectedGatewayRuntimeFailureDoesNotEscapeOrCreateIdentity() {
        gateway.runtimeFailure = new IllegalStateException("connection unavailable");

        assertEquals(StudentLoginService.LoginStatus.SERVICE_UNAVAILABLE,
                loginService.login(STUDENT_ID, "secret".toCharArray()));

        assertTrue(sessionState.studentIdentity().isEmpty());
    }

    @Test
    void reportsLockedAccountWithoutAttachingIdentity() {
        gateway.response = AuthenticationResponse.accountLocked();

        assertEquals(StudentLoginService.LoginStatus.ACCOUNT_LOCKED,
                loginService.login(STUDENT_ID, "secret".toCharArray()));

        assertTrue(sessionState.studentIdentity().isEmpty());
    }

    @Test
    void rejectsAuthenticatedAccountWithNonStudentRole() {
        AuthenticationResponse.Account teacher = new AuthenticationResponse.Account(
                "teacher-1", "teacher-1", "Teacher", AuthenticationResponse.Role.TEACHER, "session-2");
        gateway.response = AuthenticationResponse.authenticated(teacher);

        assertEquals(StudentLoginService.LoginStatus.WRONG_ROLE,
                loginService.login(STUDENT_ID, "secret".toCharArray()));

        assertTrue(sessionState.studentIdentity().isEmpty());
    }

    @Test
    void logoutClearsIdentityAndKeepsClientOnline() {
        sessionState.markOnline();
        sessionState.attachStudentIdentity(studentIdentity());

        assertEquals(StudentLoginService.LogoutStatus.LOGGED_OUT, loginService.logout());

        assertEquals("session-1", gateway.loggedOutSessionId);
        assertTrue(sessionState.studentIdentity().isEmpty());
        assertTrue(sessionState.isOnline());
    }

    @Test
    void clearsLocalIdentityWhenRemoteLogoutCannotBeReached() {
        sessionState.markOnline();
        sessionState.attachStudentIdentity(studentIdentity());
        gateway.logoutException = new AuthenticationException("offline");

        assertEquals(StudentLoginService.LogoutStatus.LOGGED_OUT_REMOTELY_UNAVAILABLE,
                loginService.logout());

        assertTrue(sessionState.studentIdentity().isEmpty());
        assertTrue(sessionState.isOnline());
    }

    @Test
    void clientCanBeOnlineWithoutAnAuthenticatedStudent() {
        sessionState.markOnline();

        assertTrue(sessionState.isOnline());
        assertTrue(sessionState.studentIdentity().isEmpty());
        assertEquals(0, gateway.authenticationCalls);
    }

    @Test
    void viewModelExposesIdentityFeedbackAndConnectionState() {
        gateway.response = AuthenticationResponse.authenticated(STUDENT_ACCOUNT);
        sessionState.markOnline();
        StudentLoginViewModel viewModel = new StudentLoginViewModel(loginService, sessionState);

        assertEquals(StudentLoginService.LoginStatus.AUTHENTICATED,
                viewModel.submitLogin(STUDENT_ID, "secret".toCharArray()));

        assertTrue(viewModel.feedback().contains("Nguyen Van An"));
        assertEquals(STUDENT_ID, viewModel.studentIdentity().orElseThrow().studentId());
        assertTrue(viewModel.isOnline());
    }

    private static StudentIdentity studentIdentity() {
        return new StudentIdentity("account-1", STUDENT_ID, "Nguyen Van An", MACHINE_ID, "session-1");
    }

    private static final class RecordingAuthenticationGateway implements AuthenticationGateway {
        private AuthenticationResponse response = AuthenticationResponse.invalidCredentials();
        private AuthenticationException authenticationException;
        private AuthenticationException logoutException;
        private RuntimeException runtimeFailure;
        private long failureDelayMillis;
        private String receivedUsername;
        private String loggedOutSessionId;
        private char[] capturedPassword;
        private int authenticationCalls;

        @Override
        public AuthenticationResponse authenticate(AuthenticationRequest request) throws AuthenticationException {
            authenticationCalls++;
            if (runtimeFailure != null) {
                throw runtimeFailure;
            }
            if (authenticationException != null) {
                if (failureDelayMillis > 0) {
                    try {
                        Thread.sleep(failureDelayMillis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AuthenticationException("authentication interrupted", exception);
                    }
                }
                throw authenticationException;
            }
            receivedUsername = request.username();
            capturedPassword = request.copyPassword();
            return response;
        }

        @Override
        public void logout(String sessionId) throws AuthenticationException {
            loggedOutSessionId = sessionId;
            if (logoutException != null) {
                throw logoutException;
            }
        }
    }
}