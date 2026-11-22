package com.dau.labmanagement.shared.protocol;

/**
 * Message codes carried in the 2-byte type field of a TCP frame.
 *
 * <p>Frame layout: {@code [4B length][2B type][1B version][JSON payload]}.
 * This enum is the single source of truth for message codes; server and
 * client modules must not re-declare them (Docs/ke_hoach_trien_khai.md,
 * section 4 — protocol contract fixed at Sprint 0).</p>
 */
public enum MessageType {

    // Discovery: server broadcasts every 2 s so clients can find it.
    BEACON,

    // Session establishment and login.
    HELLO,
    HELLO_ACK,
    LOGIN,
    LOGIN_ACK,

    // Health: heartbeat every 10 s, carries the RTT of the previous round.
    HEARTBEAT,
    ACK,

    // Control commands issued by the server.
    LOCK,
    UNLOCK,
    PROJ_START,
    PROJ_STOP,
    MON_START,
    MON_STOP,
    REC_START,
    REC_STOP,

    // File transfer; the same messages serve push (documents) and pull (submissions).
    FILE_BEGIN,
    FILE_CHUNK,
    FILE_END,

    // Collection: client locks the submission folder before sending.
    COLLECT_START,

    // Reporting: progress, network metrics and results.
    METRIC,
    LOSS_REPORT,
    RESULT
}
