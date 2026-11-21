package com.dau.labmanagement.client.agent.file;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/** Receives a bounded binary payload without implementing socket framing or transport. */
public final class FilePushReceiver {
    private static final int BUFFER_SIZE = 32 * 1024;

    private final DeBaiStorage storage;
    private final Executor receiveExecutor;
    private final Executor cancellationExecutor;

    public FilePushReceiver(DeBaiStorage storage, Executor receiveExecutor) {
        this(storage, receiveExecutor, ForkJoinPool.commonPool());
    }

    public FilePushReceiver(DeBaiStorage storage, Executor receiveExecutor, Executor cancellationExecutor) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.receiveExecutor = Objects.requireNonNull(receiveExecutor, "receiveExecutor");
        this.cancellationExecutor = Objects.requireNonNull(cancellationExecutor, "cancellationExecutor");
    }

    /**
     * Receives exactly the declared file bytes on the supplied worker executor. The input must be
     * bounded to this payload; the receiver never probes beyond its declared length. The input
     * remains owned by the caller. The cancellation action must unblock or safely abort this payload.
     * @param input the binary payload stream from the transport adapter
     * @param metadata transport-validated filename and byte length
     * @param calculateSha256 whether to calculate a local SHA-256 digest
     * @param cancelPayload transport action that aborts this payload when the operation is cancelled
     * @return a cancellable operation with completion and worker-termination stages
     */
    public ReceiveOperation receive(
            InputStream input,
            VerifiedFileMetadata metadata,
            boolean calculateSha256,
            Runnable cancelPayload) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(cancelPayload, "cancelPayload");
        ReceiveOperation operation = new ReceiveOperation(cancelPayload, cancellationExecutor);
        try {
            receiveExecutor.execute(() -> runReceive(input, metadata, calculateSha256, operation));
        } catch (RuntimeException exception) {
            operation.fail(exception);
            operation.finishWorker();
        }
        return operation;
    }

    private void runReceive(
            InputStream input,
            VerifiedFileMetadata metadata,
            boolean calculateSha256,
            ReceiveOperation operation) {
        try {
            receiveBlocking(input, metadata, calculateSha256, operation);
        } catch (IOException | RuntimeException exception) {
            operation.fail(exception);
        } finally {
            operation.finishWorker();
        }
    }

    private void receiveBlocking(
            InputStream input,
            VerifiedFileMetadata metadata,
            boolean calculateSha256,
            ReceiveOperation operation) throws IOException {
        MessageDigest digest = calculateSha256 ? newSha256() : null;
        DeBaiStorage.PendingFile pendingFile;
        try {
            pendingFile = storage.prepare(metadata.fileName(), metadata.sizeBytes());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("File receive rejected: no valid authenticated student or filename", exception);
        }

        long bytesReceived = 0;
        try {
            ensureNotCancelled(operation);
            ensureCurrentSession(pendingFile);
            try (OutputStream output = Files.newOutputStream(
                    pendingFile.temporaryPath(), StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (bytesReceived < metadata.sizeBytes()) {
                    ensureNotCancelled(operation);
                    ensureCurrentSession(pendingFile);
                    int requested = (int) Math.min(buffer.length, metadata.sizeBytes() - bytesReceived);
                    int count = input.read(buffer, 0, requested);
                    if (count < 0) {
                        throw new EOFException("File payload ended before the declared byte count");
                    }
                    if (count == 0) {
                        int value = input.read();
                        if (value < 0) {
                            throw new EOFException("File payload ended before the declared byte count");
                        }
                        buffer[0] = (byte) value;
                        count = 1;
                    }
                    ensureNotCancelled(operation);
                    ensureCurrentSession(pendingFile);
                    output.write(buffer, 0, count);
                    if (digest != null) {
                        digest.update(buffer, 0, count);
                    }
                    bytesReceived += count;
                }

                ensureNotCancelled(operation);
                ensureCurrentSession(pendingFile);
            }

            ensureCurrentSession(pendingFile);
            long completedBytes = bytesReceived;
            operation.commit(() -> {
                Path destination = pendingFile.complete(completedBytes);
                Optional<String> sha256 = digest == null
                        ? Optional.empty()
                        : Optional.of(HexFormat.of().formatHex(digest.digest()));
                return new ReceiveResult(destination, completedBytes, sha256);
            });
        } catch (IOException | RuntimeException exception) {
            try {
                pendingFile.fail();
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private void ensureCurrentSession(DeBaiStorage.PendingFile pendingFile) throws IOException {
        try {
            if (!storage.isCurrentStudent(pendingFile)) {
                throw new IOException("Student logged out or session changed during file receive");
            }
        } catch (RuntimeException exception) {
            throw new IOException("Unable to verify the current student session", exception);
        }
    }

    private static void ensureNotCancelled(ReceiveOperation operation) {
        if (operation.isCancellationRequested()) {
            throw new CancellationException("File receive was cancelled");
        }
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record ReceiveResult(Path destinationPath, long bytesReceived, Optional<String> sha256) {
        public ReceiveResult {
            Objects.requireNonNull(destinationPath, "destinationPath");
            Objects.requireNonNull(sha256, "sha256");
        }
    }

    public static final class ReceiveOperation {
        private final CompletableFuture<ReceiveResult> completion = new CompletableFuture<>();
        private final CompletableFuture<Void> workerTermination = new CompletableFuture<>();
        private final Runnable cancelPayload;
        private final Executor cancellationExecutor;
        private final AtomicReference<RuntimeException> cancellationFailure = new AtomicReference<>();
        private boolean cancellationRequested;

        private ReceiveOperation(Runnable cancelPayload, Executor cancellationExecutor) {
            this.cancelPayload = cancelPayload;
            this.cancellationExecutor = cancellationExecutor;
        }

        public CompletionStage<ReceiveResult> completion() {
            return completion.minimalCompletionStage();
        }

        public CompletionStage<Void> workerTermination() {
            return workerTermination.minimalCompletionStage();
        }

        public Optional<RuntimeException> cancellationFailure() {
            return Optional.ofNullable(cancellationFailure.get());
        }

        public synchronized boolean isCancellationRequested() {
            return cancellationRequested;
        }

        public boolean cancel() {
            synchronized (this) {
                if (completion.isDone()) {
                    return false;
                }
                cancellationRequested = true;
                completion.cancel(false);
            }
            try {
                cancellationExecutor.execute(() -> {
                    try {
                        cancelPayload.run();
                    } catch (RuntimeException exception) {
                        cancellationFailure.set(exception);
                    }
                });
            } catch (RuntimeException exception) {
                cancellationFailure.set(exception);
            }
            return true;
        }

        private synchronized void commit(ReceiveCommit commit) throws IOException {
            if (cancellationRequested) {
                throw new CancellationException("File receive was cancelled");
            }
            completion.complete(commit.finish());
        }

        private synchronized void fail(Throwable exception) {
            if (!completion.isDone()) {
                completion.completeExceptionally(exception);
            }
        }

        private void finishWorker() {
            workerTermination.complete(null);
        }
    }

    @FunctionalInterface
    private interface ReceiveCommit {
        ReceiveResult finish() throws IOException;
    }
}