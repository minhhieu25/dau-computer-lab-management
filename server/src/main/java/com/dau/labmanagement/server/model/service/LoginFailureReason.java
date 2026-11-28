package com.dau.labmanagement.server.model.service;

/**
 * Why a Server login attempt was rejected. Each reason carries the message
 * shown on the SC_001 form so the controller does not invent wording.
 */
public enum LoginFailureReason {

    /** Username or password is empty. */
    EMPTY_INPUT("Vui lòng nhập tài khoản và mật khẩu."),

    /** Unknown username, soft-deleted account or wrong password. */
    INVALID_CREDENTIALS("Tài khoản hoặc mật khẩu không đúng."),

    /** Credentials are correct but the account is locked by an admin. */
    ACCOUNT_LOCKED("Tài khoản đã bị khóa. Liên hệ Admin để mở khóa."),

    /** Students may only log in on the Client (SC_002), not on the Server. */
    ROLE_NOT_ALLOWED("Tài khoản Sinh viên không được đăng nhập phần mềm Server."),

    /** The database is not configured or unreachable. */
    SYSTEM_UNAVAILABLE("Không truy cập được cơ sở dữ liệu. Kiểm tra cấu hình DB rồi thử lại.");

    private final String message;

    LoginFailureReason(String message) {
        this.message = message;
    }

    /** Returns the user-facing Vietnamese message for this reason. */
    public String message() {
        return message;
    }
}
