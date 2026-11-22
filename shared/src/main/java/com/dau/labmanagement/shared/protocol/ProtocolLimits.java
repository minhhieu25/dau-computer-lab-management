package com.dau.labmanagement.shared.protocol;

public final class ProtocolLimits {

    public static final int MAX_TCP_FRAME_LENGTH = 1_048_576;
    public static final int MAX_UDP_DATAGRAM_LENGTH = 1_200;

    private ProtocolLimits() {
    }
}
