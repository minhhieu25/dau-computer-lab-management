package com.dau.labmanagement.shared.auth;

/** Indicates that an authentication adapter could not complete its request. */
public class AuthenticationException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an adapter failure with a diagnostic message.
     * @param message the diagnostic message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates an adapter failure with a diagnostic message and cause.
     * @param message the diagnostic message
     * @param cause the underlying failure
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}