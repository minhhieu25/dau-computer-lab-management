package com.dau.labmanagement.shared.protocol.codec;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolLimits;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class TcpFrameCodec {

    private static final int TCP_HEADER_LENGTH = Short.BYTES + Byte.BYTES;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TcpFrameCodec() {
    }

    /**
     * Reads one frame. Returns {@code null} only when the stream ends before a
     * new frame begins; a truncated frame is reported as an I/O error.
     */
    public static ProtocolFrame read(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        DataInputStream dataInput = new DataInputStream(input);
        int firstLengthByte = dataInput.read();
        if (firstLengthByte == -1) {
            return null;
        }

        byte[] remainingLengthBytes = new byte[Integer.BYTES - 1];
        dataInput.readFully(remainingLengthBytes);
        int frameLength = (firstLengthByte << 24)
                | ((remainingLengthBytes[0] & 0xFF) << 16)
                | ((remainingLengthBytes[1] & 0xFF) << 8)
                | (remainingLengthBytes[2] & 0xFF);
        validateFrameLength(frameLength);

        byte[] frameBody = new byte[frameLength];
        dataInput.readFully(frameBody);
        return decodeBody(frameBody, TCP_HEADER_LENGTH);
    }

    public static void write(OutputStream output, ProtocolFrame frame) throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(frame, "frame");

        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(frame.payload());
        int frameLength = TCP_HEADER_LENGTH + payload.length;
        validateFrameLength(frameLength);

        DataOutputStream dataOutput = new DataOutputStream(output);
        dataOutput.writeInt(frameLength);
        dataOutput.writeShort(frame.type().code());
        dataOutput.writeByte(frame.version());
        dataOutput.write(payload);
        dataOutput.flush();
    }

    public static <T> T payloadAs(ProtocolFrame frame, Class<T> payloadType) throws JsonProcessingException {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(payloadType, "payloadType");
        return OBJECT_MAPPER.treeToValue(frame.payload(), payloadType);
    }

    private static ProtocolFrame decodeBody(byte[] body, int headerLength) throws IOException {
        if (body.length < headerLength) {
            throw new IOException("Protocol frame is shorter than its header");
        }

        int messageCode = ((body[0] & 0xFF) << 8) | (body[1] & 0xFF);
        int version = body[2] & 0xFF;
        MessageType messageType;
        try {
            messageType = MessageType.fromCode(messageCode);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }

        byte[] payloadBytes = new byte[body.length - headerLength];
        System.arraycopy(body, headerLength, payloadBytes, 0, payloadBytes.length);
        JsonNode payload;
        try {
            payload = OBJECT_MAPPER.readTree(payloadBytes);
        } catch (JsonProcessingException exception) {
            throw new IOException("Invalid JSON in protocol frame", exception);
        }
        if (payload == null || !payload.isObject()) {
            throw new IOException("Protocol payload must be a JSON object");
        }

        return new ProtocolFrame(messageType, version, payload);
    }

    private static void validateFrameLength(int frameLength) throws IOException {
        if (frameLength < TCP_HEADER_LENGTH || frameLength > ProtocolLimits.MAX_TCP_FRAME_LENGTH) {
            throw new IOException("Invalid protocol frame length: " + frameLength);
        }
    }
}
