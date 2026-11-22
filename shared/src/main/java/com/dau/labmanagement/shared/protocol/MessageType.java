package com.dau.labmanagement.shared.protocol;

import java.util.HashMap;
import java.util.Map;

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
    BEACON(1),

    // Session establishment and login.
    HELLO(2),
    HELLO_ACK(3),
    LOGIN(4),
    LOGIN_ACK(5),

    // Health: heartbeat every 10 s, carries the RTT of the previous round.
    HEARTBEAT(6),
    ACK(7),

    // Control commands issued by the server.
    LOCK(8),
    UNLOCK(9),
    PROJ_START(10),
    PROJ_STOP(11),
    MON_START(12),
    MON_STOP(13),
    REC_START(14),
    REC_STOP(15),

    // File transfer; the same messages serve push (documents) and pull (submissions).
    FILE_BEGIN(16),
    FILE_CHUNK(17),
    FILE_END(18),

    // Collection: client locks the submission folder before sending.
    COLLECT_START(19),

    // Reporting: progress, network metrics and results.
    METRIC(20),
    LOSS_REPORT(21),
    RESULT(22);

    private static final Map<Integer, MessageType> BY_CODE = createCodeMap();

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static MessageType fromCode(int code) {
        MessageType messageType = BY_CODE.get(code);
        if (messageType == null) {
            throw new IllegalArgumentException("Unknown protocol message code: " + code);
        }
        return messageType;
    }

    private static Map<Integer, MessageType> createCodeMap() {
        Map<Integer, MessageType> codeMap = new HashMap<>();
        for (MessageType messageType : values()) {
            MessageType previous = codeMap.put(messageType.code, messageType);
            if (previous != null) {
                throw new IllegalStateException("Duplicate protocol message code: " + messageType.code);
            }
        }
        return Map.copyOf(codeMap);
    }
}
