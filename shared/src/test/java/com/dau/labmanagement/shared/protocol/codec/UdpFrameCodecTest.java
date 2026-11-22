package com.dau.labmanagement.shared.protocol.codec;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UdpFrameCodecTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void encodesAndDecodesAHeartbeatDatagram() throws Exception {
        ProtocolFrame expected = new ProtocolFrame(
                MessageType.HEARTBEAT,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(new HeartbeatMessage("client-1", 42, 123456L)));

        byte[] encoded = UdpFrameCodec.encode(expected);
        ProtocolFrame actual = UdpFrameCodec.decode(encoded, encoded.length);

        assertEquals(expected.type(), actual.type());
        assertEquals(expected.version(), actual.version());
        assertEquals(new HeartbeatMessage("client-1", 42, 123456L),
                UdpFrameCodec.payloadAs(actual, HeartbeatMessage.class));
    }

    @Test
    void rejectsTruncatedAndOversizedDatagrams() {
        assertThrows(java.io.IOException.class, () -> UdpFrameCodec.decode(new byte[2], 2));
        assertThrows(java.io.IOException.class,
                () -> UdpFrameCodec.decode(new byte[1201], 1201));
    }
}
