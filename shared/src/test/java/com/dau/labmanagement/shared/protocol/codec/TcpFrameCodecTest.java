package com.dau.labmanagement.shared.protocol.codec;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TcpFrameCodecTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void encodesAndDecodesAFrameWithTypedPayload() throws Exception {
        ProtocolFrame expected = new ProtocolFrame(
                MessageType.HELLO,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(new HelloMessage("client-1", "lab-pc-01")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        TcpFrameCodec.write(output, expected);

        ProtocolFrame actual = TcpFrameCodec.read(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(expected, actual);
        assertEquals(new HelloMessage("client-1", "lab-pc-01"),
                TcpFrameCodec.payloadAs(actual, HelloMessage.class));
    }

    @Test
    void returnsNullOnlyWhenTheStreamEndsBetweenFrames() throws Exception {
        assertNull(TcpFrameCodec.read(new ByteArrayInputStream(new byte[0])));
        assertThrows(IOException.class,
                () -> TcpFrameCodec.read(new ByteArrayInputStream(new byte[] {0, 0})));
    }

    @Test
    void rejectsFrameLengthsOutsideTheSupportedRange() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.writeInt(2);
        }

        assertThrows(IOException.class,
                () -> TcpFrameCodec.read(new ByteArrayInputStream(output.toByteArray())));
    }

    @Test
    void rejectsUnknownMessageCodes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.writeInt(3);
            data.writeShort(0xFFFF);
            data.writeByte(ProtocolVersion.CURRENT);
        }

        assertThrows(IOException.class,
                () -> TcpFrameCodec.read(new ByteArrayInputStream(output.toByteArray())));
    }
}
