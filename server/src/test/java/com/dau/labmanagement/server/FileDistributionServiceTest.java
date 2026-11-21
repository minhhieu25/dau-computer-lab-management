package com.dau.labmanagement.server;

import com.dau.labmanagement.server.model.state.TransferProgress;
import com.dau.labmanagement.server.model.state.TransferStatus;
import com.dau.labmanagement.server.service.filepush.FileDistributionService;
import com.dau.labmanagement.server.service.filepush.FileTransferTransport;
import com.dau.labmanagement.server.service.filepush.RecipientLookupException;
import com.dau.labmanagement.server.service.filepush.RecipientSession;
import com.dau.labmanagement.server.service.filepush.RecipientSessionDirectory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FileDistributionServiceTest {
    @TempDir
    Path temporaryDirectory;

    private Path sourceFile;
    private TestRecipientDirectory directory;
    private TestTransport transport;
    private FileDistributionService service;

    @BeforeEach
    void setUp() throws IOException {
        sourceFile = temporaryDirectory.resolve("assignment.bin");
        Files.write(sourceFile, new byte[1_000]);
        directory = new TestRecipientDirectory();
        transport = new TestTransport();
        service = new FileDistributionService(directory, transport);
    }

    @Test
    void rejectsOfflineUnauthenticatedAndInvalidSessionsWithoutStartingTransport() throws IOException {
        directory.sessions.put("offline", recipient("offline", false, true, true));
        directory.sessions.put("guest", recipient("guest", true, false, false));
        directory.sessions.put("stale", recipient("stale", true, true, false));

        List<TransferProgress> results = service.distribute(sourceFile, List.of("offline", "guest", "stale"));

        assertEquals(List.of(TransferStatus.REJECTED, TransferStatus.REJECTED, TransferStatus.REJECTED),
                results.stream().map(TransferProgress::status).toList());
        assertEquals(List.of("CLIENT_OFFLINE", "STUDENT_NOT_LOGGED_IN", "INVALID_STUDENT_SESSION"),
                results.stream().map(progress -> progress.failureReason().orElseThrow()).toList());
        assertEquals(0, transport.started.size());
    }

    @Test
    void startsSeparateProgressForEachEligibleMachineAndDeduplicatesSelection() throws IOException {
        directory.sessions.put("pc-1", recipient("pc-1", true, true, true));
        directory.sessions.put("pc-2", recipient("pc-2", true, true, true));
        transport.behaviors.put("pc-1", Behavior.PARTIAL);
        transport.behaviors.put("pc-2", Behavior.COMPLETE);

        List<TransferProgress> results = service.distribute(sourceFile, List.of("pc-1", "pc-2", "pc-1"));

        assertEquals(2, results.size());
        assertNotEquals(results.get(0).transferId(), results.get(1).transferId());
        assertEquals("student-pc-1", results.get(0).studentId().orElseThrow());
        assertEquals(500, results.get(0).bytesSent());
        assertEquals(200, results.get(0).bytesAcknowledged());
        assertEquals(TransferStatus.TRANSFERRING, results.get(0).status());
        assertEquals(TransferStatus.COMPLETED, results.get(1).status());
        assertEquals(List.of("pc-1", "pc-2"), transport.started);
    }

    @Test
    void isolatesTransportFailureAndContinuesWithOtherMachines() throws IOException {
        directory.sessions.put("pc-fail", recipient("pc-fail", true, true, true));
        directory.sessions.put("pc-ok", recipient("pc-ok", true, true, true));
        transport.behaviors.put("pc-fail", Behavior.FAIL);
        transport.behaviors.put("pc-ok", Behavior.COMPLETE);

        List<TransferProgress> results = service.distribute(sourceFile, List.of("pc-fail", "pc-ok"));

        assertEquals(TransferStatus.FAILED, results.get(0).status());
        assertEquals(500, results.get(0).bytesSent());
        assertEquals(250, results.get(0).bytesAcknowledged());
        assertEquals(Optional.empty(), results.get(0).eta());
        assertEquals(TransferStatus.COMPLETED, results.get(1).status());
        assertEquals(List.of("pc-fail", "pc-ok"), transport.started);
    }

    @Test
    void rejectsUnknownMachineAndMarksDirectoryFailurePerMachine() throws IOException {
        directory.failures.put("pc-error", new RecipientLookupException("directory unavailable"));
        directory.sessions.put("pc-ok", recipient("pc-ok", true, true, true));

        List<TransferProgress> results = service.distribute(sourceFile, List.of("unknown", "pc-error", "pc-ok"));

        assertEquals(TransferStatus.REJECTED, results.get(0).status());
        assertEquals("MACHINE_NOT_FOUND", results.get(0).failureReason().orElseThrow());
        assertEquals(TransferStatus.FAILED, results.get(1).status());
        assertEquals(TransferStatus.COMPLETED, results.get(2).status());
    }

    private static RecipientSession recipient(
            String machineId, boolean online, boolean authenticated, boolean sessionValid) {
        return new RecipientSession(
                machineId,
                online,
                authenticated,
                authenticated ? Optional.of("student-" + machineId) : Optional.empty(),
                authenticated ? Optional.of("session-" + machineId) : Optional.empty(),
                sessionValid);
    }

    private enum Behavior {
        COMPLETE,
        PARTIAL,
        FAIL
    }

    private static final class TestRecipientDirectory implements RecipientSessionDirectory {
        private final Map<String, RecipientSession> sessions = new HashMap<>();
        private final Map<String, RecipientLookupException> failures = new HashMap<>();

        @Override
        public Optional<RecipientSession> findByMachineId(String machineId) throws RecipientLookupException {
            if (failures.containsKey(machineId)) {
                throw failures.get(machineId);
            }
            return Optional.ofNullable(sessions.get(machineId));
        }
    }

    private static final class TestTransport implements FileTransferTransport {
        private final Map<String, Behavior> behaviors = new HashMap<>();
        private final java.util.ArrayList<String> started = new java.util.ArrayList<>();

        @Override
        public void startTransfer(Path sourceFile, RecipientSession recipient, TransferProgress progress)
                throws IOException {
            started.add(recipient.machineId());
            Behavior behavior = behaviors.getOrDefault(recipient.machineId(), Behavior.COMPLETE);
            if (behavior == Behavior.FAIL) {
                progress.recordBytesSent(progress.totalBytes() / 2);
                progress.acknowledgeBytesWritten(progress.totalBytes() / 4);
                throw new IOException("simulated transport start failure");
            }

            long totalBytes = progress.totalBytes();
            progress.recordBytesSent(behavior == Behavior.PARTIAL ? totalBytes / 2 : totalBytes);
            long acknowledged = behavior == Behavior.PARTIAL ? totalBytes / 5 : totalBytes;
            if (acknowledged > 0) {
                progress.acknowledgeBytesWritten(acknowledged);
            }
            if (behavior == Behavior.COMPLETE) {
                progress.complete();
            }
        }
    }
}