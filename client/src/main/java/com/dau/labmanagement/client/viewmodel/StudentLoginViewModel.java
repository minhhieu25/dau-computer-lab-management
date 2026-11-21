package com.dau.labmanagement.client.viewmodel;

import com.dau.labmanagement.client.model.entity.StudentIdentity;
import com.dau.labmanagement.client.model.state.ClientSessionState;
import com.dau.labmanagement.client.service.auth.StudentLoginService;
import java.util.Objects;
import java.util.Optional;

/** Exposes student login feedback and session state for a future JavaFX view/controller. */
public final class StudentLoginViewModel {
    private final StudentLoginService loginService;
    private final ClientSessionState sessionState;
    private String feedback = "";

    /**
     * Creates a view-model facade for a future JavaFX login view.
     * @param loginService the student authentication service
     * @param sessionState the client connection and identity state
     */
    public StudentLoginViewModel(StudentLoginService loginService, ClientSessionState sessionState) {
        this.loginService = Objects.requireNonNull(loginService, "loginService");
        this.sessionState = Objects.requireNonNull(sessionState, "sessionState");
    }

    /**
     * Submits credentials and updates user-facing feedback.
     * @param studentId the student's MSSV
     * @param password the password characters
     * @return the login outcome
     */
    public synchronized StudentLoginService.LoginStatus submitLogin(String studentId, char[] password) {
        StudentLoginService.LoginStatus status = loginService.login(studentId, password);
        feedback = switch (status) {
            case AUTHENTICATED -> "Đăng nhập thành công: "
                    + sessionState.studentIdentity().map(StudentIdentity::displayName).orElse("");
            case MISSING_STUDENT_ID -> "Vui lòng nhập MSSV.";
            case MISSING_PASSWORD -> "Vui lòng nhập mật khẩu.";
            case INVALID_CREDENTIALS -> "MSSV hoặc mật khẩu không đúng.";
            case ACCOUNT_LOCKED -> "Tài khoản đang bị khóa.";
            case WRONG_ROLE -> "Tài khoản không có quyền đăng nhập sinh viên.";
            case ALREADY_AUTHENTICATED -> "Đã có tài khoản đăng nhập trên máy này.";
            case SERVICE_UNAVAILABLE -> "Không thể kết nối dịch vụ xác thực.";
        };
        return status;
    }

    /**
     * Logs out and updates user-facing feedback.
     * @return the logout outcome
     */
    public synchronized StudentLoginService.LogoutStatus logout() {
        StudentLoginService.LogoutStatus status = loginService.logout();
        feedback = switch (status) {
            case LOGGED_OUT -> "Đã đăng xuất.";
            case LOGGED_OUT_REMOTELY_UNAVAILABLE -> "Đã đăng xuất khỏi máy; máy chủ chưa xác nhận.";
            case NOT_AUTHENTICATED -> "Chưa có sinh viên đăng nhập.";
        };
        return status;
    }

    /**
     * Returns the most recent user-facing result message.
     * @return the feedback text
     */
    public synchronized String feedback() {
        return feedback;
    }

    /**
     * Returns the current authenticated student identity, if present.
     * @return the current identity or an empty optional
     */
    public Optional<StudentIdentity> studentIdentity() {
        return sessionState.studentIdentity();
    }

    /**
     * Returns the client connection state independently of authentication.
     * @return true when the client is online
     */
    public boolean isOnline() {
        return sessionState.isOnline();
    }
}