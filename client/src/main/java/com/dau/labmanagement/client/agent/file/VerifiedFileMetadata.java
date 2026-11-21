package com.dau.labmanagement.client.agent.file;

import java.util.Objects;

/** File name and length validated by the transport adapter, not a wire-protocol definition. */
public record VerifiedFileMetadata(String fileName, long sizeBytes) {
    public VerifiedFileMetadata {
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
    }
}