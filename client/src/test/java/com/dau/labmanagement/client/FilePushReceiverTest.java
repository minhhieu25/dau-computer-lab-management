package com.dau.labmanagement.client;

import com.dau.labmanagement.client.agent.file.CurrentStudentIdProvider;
import com.dau.labmanagement.client.agent.file.DeBaiStorage;
import com.dau.labmanagement.client.agent.file.FilePushReceiver;
import com.dau.labmanagement.client.agent.file.VerifiedFileMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePushReceiverTest {
    private static final String STUDENT_ID = "20260001";

    @TempDir
    Path temporaryDirectory;

    private AtomicReference<String> currentStudentId;
    private ExecutorService receiveExecutor;
    private DeBaiStorage storage;
    private FilePushReceiver receiver;

    @BeforeEach
    void setUp() {
        currentStudentId = new AtomicReference<>(STUDENT_ID);
        CurrentStudentIdProvider identityProvider = () -> Optional.ofNullable(currentStudentId.get());
        storage = new DeBaiStorage(temporaryDirectory, identityProvider);
        receiveExecutor = Executors.newSingleThreadExecutor();
        receiver = new FilePushReceiver(storage, receiveExecutor);
    }

    @AfterEach
    void tearDown() {
        receiveExecutor.shutdownNow();
    }

    @Test
    void receivesPdfBytesWithoutTextConversion() throws IOException {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x37, 0, -1};

        FilePushReceiver.ReceiveResult result = receive("brief.pdf", pdfBytes, true);

        assertArrayEquals(pdfBytes, Files.readAllBytes(result.destinationPath()));
    }

    @Test
    void receivesZipBytesWithoutDependingOnExtensionContents() throws IOException {
        byte[] zipBytes = {0x50, 0x4b, 0x03, 0x04, 0, 0, 0x50, 0x4b, 0x05, 0x06};

        FilePushReceiver.ReceiveResult result = receive("model.zip", zipBytes, false);

        assertArrayEquals(zipBytes, Files.readAllBytes(result.destinationPath()));
        assertTrue(result.sha256().isEmpty());
    }

    @Test
    void receivesBinaryPayloadLargerThanBuffer() throws IOException {
        byte[] binaryFile = new byte[1_500_000];
        new java.util.Random(23).nextBytes(binaryFile);

        FilePushReceiver.ReceiveResult result = receive("drawing.cad", binaryFile, false);

        assertArrayEquals(binaryFile, Files.readAllBytes(result.destinationPath()));
        assertEquals(binaryFile.length, result.bytesReceived());
    }

    @Test
    void receivesEmptyFile() throws IOException {
        FilePushReceiver.ReceiveResult result = receive("empty.docx", new byte[0], false);

        assertEquals(0, Files.size(result.destinationPath()));
        assertEquals(0, result.bytesReceived());
    }

    @Test
    void rejectsShortStreamAndDeletesPartialFile() {
        assertReceiveFails(new ByteArrayInputStream(new byte[]{1, 2}), "short.pdf", 3);

        assertFalse(Files.exists(destination("short.pdf")));
        assertFalse(Files.exists(partial("short.pdf")));
    }

    @Test
    void leavesFollowingFrameBytesUnreadAtDeclaredPayloadBoundary() throws IOException {
        ByteArrayInputStream persistentPayload = new ByteArrayInputStream(new byte[]{1, 2, 3});

        FilePushReceiver.ReceiveResult result = receive(persistentPayload, "bounded.zip", 2, false);

        assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(result.destinationPath()));
        assertEquals(3, persistentPayload.read());
    }

    @Test
    void refusesDuplicateFilenameWithoutOverwritingExistingBytes() throws IOException {
        byte[] original = {3, 1, 4};
        receive("same.bin", original, false);

        assertReceiveFails(new ByteArrayInputStream(new byte[]{9, 9, 9}), "same.bin", 3);

        assertArrayEquals(original, Files.readAllBytes(destination("same.bin")));
    }

    @Test
    void rejectsFilenamePathTraversal() {
        assertReceiveFails(new ByteArrayInputStream(new byte[]{1}), "../outside.bin", 1);
        assertReceiveFails(new ByteArrayInputStream(new byte[]{1}), "NUL.txt", 1);

        assertFalse(Files.exists(temporaryDirectory.resolve("outside.bin")));
    }

    @Test
    void rejectsReceiveWhenStudentIsNotLoggedIn() {
        currentStudentId.set(null);

        assertReceiveFails(new ByteArrayInputStream(new byte[]{1}), "no-login.bin", 1);
    }

    @Test
    void logoutDuringReceiveAbortsAndRemovesPartialFile() {
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        Path destination = destination("logout.bin");
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        InputStream loggingOutStream = new ByteArrayInputStream(payload) {
            private boolean loggedOut;

            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                int count = super.read(buffer, offset, length);
                if (!loggedOut && count > 0) {
                    currentStudentId.set(null);
                    loggedOut = true;
                }
                return count;
            }
        };

        assertReceiveFails(loggingOutStream, "logout.bin", payload.length);

        assertFalse(Files.exists(destination));
        assertFalse(Files.exists(partial));
    }

    @Test
    void cancellationAbortsPayloadAndDeletesPartialFile() throws Exception {
        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch resumeRead = new CountDownLatch(1);
        AtomicReference<Thread> cancellationThread = new AtomicReference<>();
        byte[] payload = new byte[16];
        Path destination = destination("cancelled.bin");
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        InputStream blockingStream = new ByteArrayInputStream(payload) {
            private boolean blocked;

            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                int count = super.read(buffer, offset, length);
                if (!blocked && count > 0) {
                    blocked = true;
                    readStarted.countDown();
                    try {
                        resumeRead.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                }
                return count;
            }
        };

        FilePushReceiver.ReceiveOperation operation = receiver.receive(
                blockingStream,
                new VerifiedFileMetadata("cancelled.bin", payload.length),
                false,
                () -> {
                    cancellationThread.set(Thread.currentThread());
                    resumeRead.countDown();
                });

        assertTrue(readStarted.await(2, TimeUnit.SECONDS));
        assertTrue(Files.exists(partial));
        assertTrue(operation.cancel());
        assertTrue(operation.isCancellationRequested());
        CompletionException cancellation = assertThrows(CompletionException.class,
            () -> operation.completion().toCompletableFuture().join());
        assertTrue(cancellation.getCause() instanceof CancellationException);
        operation.workerTermination().toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertNotEquals(Thread.currentThread(), cancellationThread.get());
        assertFalse(Files.exists(partial));
        assertFalse(Files.exists(destination));
    }

    @Test
    void changedStudentSessionDoesNotCompleteFileUnderEitherStudent() {
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        Path firstDestination = destination("session-change.bin");
        Path firstPartial = firstDestination.resolveSibling(firstDestination.getFileName() + ".part");
        Path secondDestination = temporaryDirectory.resolve("20260002")
                .resolve("DeBai").resolve("session-change.bin");
        InputStream switchingStream = new ByteArrayInputStream(payload) {
            private boolean switched;

            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                int count = super.read(buffer, offset, length);
                if (!switched && count > 0) {
                    currentStudentId.set("20260002");
                    switched = true;
                }
                return count;
            }
        };

        assertReceiveFails(switchingStream, "session-change.bin", payload.length);

        assertFalse(Files.exists(firstDestination));
        assertFalse(Files.exists(firstPartial));
        assertFalse(Files.exists(secondDestination));
    }

    @Test
    void readsPayloadOnInjectedWorkerExecutor() throws IOException {
        AtomicReference<Thread> readingThread = new AtomicReference<>();
        InputStream trackingStream = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                readingThread.set(Thread.currentThread());
                return super.read(buffer, offset, length);
            }
        };
        Thread callingThread = Thread.currentThread();

        receive(trackingStream, "background.bin", 3, false);

        assertNotEquals(callingThread, readingThread.get());
    }

    @Test
    void differentStudentSessionsUseDifferentDirectories() throws IOException {
        byte[] payload = {7, 8, 9};
        Path firstDestination = receive("work.zip", payload, false).destinationPath();
        currentStudentId.set("20260002");
        Path secondDestination = receive("work.zip", payload, false).destinationPath();

        assertNotEquals(firstDestination, secondDestination);
        assertTrue(firstDestination.toString().contains(STUDENT_ID));
        assertTrue(secondDestination.toString().contains("20260002"));
        assertArrayEquals(payload, Files.readAllBytes(firstDestination));
        assertArrayEquals(payload, Files.readAllBytes(secondDestination));
    }

    @Test
    void optionalSha256MatchesSourceAndDestinationBytes() throws Exception {
        byte[] sourceBytes = new byte[128_000];
        new java.util.Random(71).nextBytes(sourceBytes);

        FilePushReceiver.ReceiveResult result = receive("hash-test.bin", sourceBytes, true);
        String sourceHash = sha256(sourceBytes);
        String destinationHash = sha256(Files.readAllBytes(result.destinationPath()));

        assertEquals(sourceHash, destinationHash);
        assertEquals(Optional.of(sourceHash), result.sha256());
    }

    private FilePushReceiver.ReceiveResult receive(String fileName, byte[] payload, boolean calculateSha256)
            throws IOException {
        return receive(new ByteArrayInputStream(payload), fileName, payload.length, calculateSha256);
    }

    private FilePushReceiver.ReceiveResult receive(InputStream input, String fileName, long sizeBytes,
                                                    boolean calculateSha256) throws IOException {
        try {
            return receiver.receive(input, new VerifiedFileMetadata(fileName, sizeBytes),
                    calculateSha256, () -> { }).completion().toCompletableFuture().join();
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }

    private void assertReceiveFails(InputStream input, String fileName, long sizeBytes) {
        assertThrows(IOException.class, () -> receive(input, fileName, sizeBytes, false));
    }

    private Path destination(String fileName) {
        return storage.resolveDestination(fileName);
    }

    private Path partial(String fileName) {
        Path destination = destination(fileName);
        return destination.resolveSibling(destination.getFileName() + ".part");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}