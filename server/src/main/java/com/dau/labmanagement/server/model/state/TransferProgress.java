package com.dau.labmanagement.server.model.state;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Thread-safe progress and outcome for one target client's file transfer. */
public final class TransferProgress {
    private final UUID transferId;
    private final String machineId;
    private final Optional<String> studentId;
    private final String fileName;
    private final long totalBytes;
    private final LongSupplier nanoClock;
    private long bytesSent;
    private long bytesAcknowledged;
    private long startedAtNanos;
    private boolean started;
    private TransferStatus status = TransferStatus.PENDING;
    private String failureReason;

    public TransferProgress(
            UUID transferId,
            String machineId,
            Optional<String> studentId,
            String fileName,
            long totalBytes) {
        this(transferId, machineId, studentId, fileName, totalBytes, System::nanoTime);
    }

    public TransferProgress(
            UUID transferId,
            String machineId,
            Optional<String> studentId,
            String fileName,
            long totalBytes,
            LongSupplier nanoClock) {
        this.transferId = Objects.requireNonNull(transferId, "transferId");
        this.machineId = requireText(machineId, "machineId");
        this.studentId = Objects.requireNonNull(studentId, "studentId")
                .map(value -> requireText(value, "studentId"));
        this.fileName = requireText(fileName, "fileName");
        if (totalBytes < 0) {
            throw new IllegalArgumentException("totalBytes must not be negative");
        }
        this.totalBytes = totalBytes;
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    public UUID transferId() {
        return transferId;
    }

    public String machineId() {
        return machineId;
    }

    public Optional<String> studentId() {
        return studentId;
    }

    public String fileName() {
        return fileName;
    }

    public long totalBytes() {
        return totalBytes;
    }

    public synchronized long bytesSent() {
        return bytesSent;
    }

    public synchronized long bytesAcknowledged() {
        return bytesAcknowledged;
    }

    public synchronized TransferStatus status() {
        return status;
    }

    public synchronized Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    /** Marks the transfer as started and begins monotonic time measurements. */
    public synchronized void start() {
        requireStatus(TransferStatus.PENDING);
        started = true;
        startedAtNanos = nanoClock.getAsLong();
        status = TransferStatus.TRANSFERRING;
    }

    /** Records bytes written to the transport, which are not necessarily persisted by the client. */
    public synchronized void recordBytesSent(long count) {
        requireActive();
        requirePositiveCount(count);
        if (count > totalBytes - bytesSent) {
            throw new IllegalArgumentException("bytesSent cannot exceed totalBytes");
        }
        if (!started) {
            started = true;
            startedAtNanos = nanoClock.getAsLong();
            status = TransferStatus.TRANSFERRING;
        }
        bytesSent += count;
    }

    /** Records bytes the client has acknowledged as written to disk. */
    public synchronized void acknowledgeBytesWritten(long count) {
        requireActive();
        requirePositiveCount(count);
        if (count > bytesSent - bytesAcknowledged) {
            throw new IllegalArgumentException("Acknowledged bytes cannot exceed bytes sent");
        }
        bytesAcknowledged += count;
    }

    public synchronized double speedMbps() {
        long elapsedNanos = elapsedNanos();
        if (!started || elapsedNanos <= 0) {
            return 0.0;
        }
        return bytesSent * 8_000.0 / elapsedNanos;
    }

    /** Returns completion percentage based on client-acknowledged disk writes. */
    public synchronized double percentComplete() {
        if (totalBytes == 0) {
            return status == TransferStatus.COMPLETED ? 100.0 : 0.0;
        }
        return Math.min(100.0, bytesAcknowledged * 100.0 / totalBytes);
    }

    /** Returns an ETA based on acknowledged bytes, or empty before an ACK rate can be measured. */
    public synchronized Optional<Duration> eta() {
        if (status == TransferStatus.COMPLETED) {
            return Optional.of(Duration.ZERO);
        }
        if (status != TransferStatus.PENDING && status != TransferStatus.TRANSFERRING) {
            return Optional.empty();
        }
        if (totalBytes == 0 || totalBytes == bytesAcknowledged) {
            return Optional.of(Duration.ZERO);
        }
        if (!started || bytesAcknowledged == 0) {
            return Optional.empty();
        }
        long elapsedNanos = elapsedNanos();
        if (elapsedNanos <= 0) {
            return Optional.empty();
        }
        long remainingBytes = totalBytes - bytesAcknowledged;
        double estimateNanos = (double) remainingBytes * elapsedNanos / bytesAcknowledged;
        if (!Double.isFinite(estimateNanos) || estimateNanos >= Long.MAX_VALUE) {
            return Optional.of(Duration.ofNanos(Long.MAX_VALUE));
        }
        return Optional.of(Duration.ofNanos(Math.max(0, (long) Math.ceil(estimateNanos))));
    }

    /** Completes only after the entire source was sent and acknowledged as written. */
    public synchronized void complete() {
        requireActive();
        if (bytesSent != totalBytes || bytesAcknowledged != totalBytes) {
            throw new IllegalStateException("Cannot complete before all bytes are sent and acknowledged");
        }
        status = TransferStatus.COMPLETED;
    }

    public synchronized void fail(String reason) {
        terminate(TransferStatus.FAILED, reason);
    }

    public synchronized void reject(String reason) {
        requireStatus(TransferStatus.PENDING);
        terminate(TransferStatus.REJECTED, reason);
    }

    public synchronized void cancel(String reason) {
        terminate(TransferStatus.CANCELLED, reason);
    }

    private void terminate(TransferStatus terminalStatus, String reason) {
        requireActive();
        status = terminalStatus;
        failureReason = requireText(reason, "reason");
    }

    private void requireActive() {
        if (status != TransferStatus.PENDING && status != TransferStatus.TRANSFERRING) {
            throw new IllegalStateException("Transfer is already terminal: " + status);
        }
    }

    private void requireStatus(TransferStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected transfer status " + expected + " but was " + status);
        }
    }

    private long elapsedNanos() {
        return Math.max(0, nanoClock.getAsLong() - startedAtNanos);
    }

    private static void requirePositiveCount(long count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Byte count must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}