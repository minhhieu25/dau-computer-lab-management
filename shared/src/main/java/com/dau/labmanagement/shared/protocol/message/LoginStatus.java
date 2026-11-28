package com.dau.labmanagement.shared.protocol.message;

/** Result of a LOGIN exchange. */
public enum LoginStatus {
    CHALLENGE,
    AUTHENTICATED,
    REJECTED
}
