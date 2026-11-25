package com.dau.labmanagement.shared.protocol;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageTypeTest {

    @Test
    void everyMessageCodeIsUniqueAndRoundTrips() {
        Set<Integer> codes = new HashSet<>();
        Arrays.stream(MessageType.values()).forEach(type -> {
            codes.add(type.code());
            assertEquals(type, MessageType.fromCode(type.code()));
        });

        assertEquals(MessageType.values().length, codes.size());
    }

    @Test
    void unknownCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MessageType.fromCode(0xFFFF));
    }
}
