package com.dau.labmanagement.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record ProtocolFrame(MessageType type, int version, JsonNode payload) {

    public ProtocolFrame {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        if (version < 0 || version > 0xFF) {
            throw new IllegalArgumentException("Protocol version must fit in one byte");
        }
    }
}
