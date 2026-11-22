package com.dau.labmanagement.shared.protocol.codec;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolLimits;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

public final class UdpFrameCodec {

    private static final int UDP_HEADER_LENGTH = Short.BYTES + Byte.BYTES;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UdpFrameCodec() {
    }

    public static byte[] encode(ProtocolFrame frame) throws IOException {
        Objects.requireNonNull(frame, "frame");
        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(frame.payload());
        int datagramLength = UDP_HEADER_LENGTH + payload.length;
        validateDatagramLength(datagramLength);

        byte[] datagram = new byte[datagramLength];
        datagram[0] = (byte) (frame.type().code() >>> 8);
        datagram[1] = (byte) frame.type().code();
        datagram[2] = (byte) frame.version();
        System.arraycopy(payload, 0, datagram, UDP_HEADER_LENGTH, payload.length);
        return datagram;
    }

    public static ProtocolFrame decode(byte[] datagram, int length) throws IOException {
        Objects.requireNonNull(datagram, "datagram");
        validateDatagramLength(length);
        if (length > datagram.length) {
            throw new IOException("Datagram length exceeds the provided buffer");
        }

        int messageCode = ((datagram[0] & 0xFF) << 8) | (datagram[1] & 0xFF);
        int version = datagram[2] & 0xFF;
        MessageType messageType;
        try {
            messageType = MessageType.fromCode(messageCode);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }

        JsonNode payload;
        try {
            payload = OBJECT_MAPPER.readTree(datagram, UDP_HEADER_LENGTH, length - UDP_HEADER_LENGTH);
        } catch (JsonProcessingException exception) {
            throw new IOException("Invalid JSON in UDP protocol frame", exception);
        }
        if (payload == null || !payload.isObject()) {
            throw new IOException("Protocol payload must be a JSON object");
        }

        return new ProtocolFrame(messageType, version, payload);
    }

    public static <T> T payloadAs(ProtocolFrame frame, Class<T> payloadType) throws JsonProcessingException {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(payloadType, "payloadType");
        return OBJECT_MAPPER.treeToValue(frame.payload(), payloadType);
    }

    private static void validateDatagramLength(int datagramLength) throws IOException {
        if (datagramLength < UDP_HEADER_LENGTH || datagramLength > ProtocolLimits.MAX_UDP_DATAGRAM_LENGTH) {
            throw new IOException("Invalid UDP protocol frame length: " + datagramLength);
        }
    }
}
