package com.dau.labmanagement.server;

import com.dau.labmanagement.server.model.state.TransferProgress;
import com.dau.labmanagement.server.model.state.TransferStatus;
import com.dau.labmanagement.server.service.filepush.FileDistributionAuthorizationException;
import com.dau.labmanagement.server.service.filepush.FileDistributionAuthorizer;
import com.dau.labmanagement.server.service.filepush.FileDistributionService;
import com.dau.labmanagement.server.service.filepush.FileTransferAttempt;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDistributionServiceTest {
    private static final String TEACHER_SESSION_ID = "teacher-session-1";
    private static final long MAX_FILE_SIZE_BYTES = 2_000;

    @TempDir
    Path temporaryDirectory;

    private Path sourceFile;
    private TestRecipientDirectory directory;
    private TestTransport transport;
    private TestTeacherAuthorizer authorizer;
    private FileDistributionService service;

    @BeforeEach
    void setUp() throws IOException {
        sourceFile = temporaryDirectory.resolve("assignment.bin");
        Files.write(sourceFile, new byte[1_000]);
        directory = new TestRecipientDirectory();
        transport = new TestTransport();
        authorizer = new TestTeacherAuthorizer();
        service = new FileDistributionService(directory, transport, authorizer, MAX_FILE_SIZE_BYTES);
    }

    @Test
    void rejectsOfflineUnauthenticatedAndInvalidSessionsWithoutStartingTransport() throws IOException {
        directory.sessions.put("offline", recipient("offline", false, true, true));
        directory.sessions.put("guest", recipient("guest", true, false, false));
        directory.sessions.put("stale", recipient("stale", true, true, false));

        List<TransferProgress> results = distribute(sourceFile, List.of("offline", "guest", "stale"));

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

        List<TransferProgress> results = distribute(sourceFile, List.of("pc-1", "pc-2", "pc-1"));

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

        List<TransferProgress> results = distribute(sourceFile, List.of("pc-fail", "pc-ok"));

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

        List<TransferProgress> results = distribute(sourceFile, List.of("unknown", "pc-error", "pc-ok"));

        assertEquals(TransferStatus.REJECTED, results.get(0).status());
        assertEquals("MACHINE_NOT_FOUND", results.get(0).failureReason().orElseThrow());
        assertEquals(TransferStatus.FAILED, results.get(1).status());
        assertEquals(TransferStatus.COMPLETED, results.get(2).status());
    }

    @Test
    void deniesUnauthorizedTeacherBeforeStartingAnyTransfer() {
        authorizer.authorized = false;
        directory.sessions.put("pc-1", recipient("pc-1", true, true, true));

        assertThrows(FileDistributionAuthorizationException.class,
                () -> service.distribute(sourceFile, List.of("pc-1"), TEACHER_SESSION_ID));

        assertEquals(0, transport.started.size());
    }

    @Test
    void rejectsMissingAndOversizeSourceFiles() throws IOException {
        Path missingFile = temporaryDirectory.resolve("missing.bin");
        Path oversizeFile = temporaryDirectory.resolve("oversize.bin");
        Files.write(oversizeFile, new byte[(int) MAX_FILE_SIZE_BYTES + 1]);

        assertThrows(IOException.class, () -> distribute(missingFile, List.of("pc-1")));
        assertThrows(IOException.class, () -> distribute(oversizeFile, List.of("pc-1")));
        assertEquals(0, transport.started.size());
    }

    @Test
    void distributesZeroLengthFileWithoutDividingByZero() throws IOException {
        Path emptyFile = temporaryDirectory.resolve("empty.zip");
        Files.createFile(emptyFile);
        directory.sessions.put("pc-empty", recipient("pc-empty", true, true, true));

        List<TransferProgress> results = distribute(emptyFile, List.of("pc-empty"));

        assertEquals(TransferStatus.COMPLETED, results.get(0).status());
        assertEquals(0, results.get(0).totalBytes());
        assertEquals(100.0, results.get(0).percentComplete());
        assertEquals(List.of("pc-empty"), transport.started);
    }

    @Test
        void cancellingOneRecipientDoesNotCancelOtherRecipients()
            throws IOException, FileDistributionAuthorizationException {
        directory.sessions.put("pc-1", recipient("pc-1", true, true, true));
        directory.sessions.put("pc-2", recipient("pc-2", true, true, true));
        transport.behaviors.put("pc-1", Behavior.PARTIAL);
        transport.behaviors.put("pc-2", Behavior.PARTIAL);

        List<FileTransferAttempt> attempts = service.distribute(
                sourceFile, List.of("pc-1", "pc-2"), TEACHER_SESSION_ID);

        assertTrue(attempts.get(0).cancel());

        assertEquals(TransferStatus.CANCELLED, attempts.get(0).progress().status());
        assertEquals(TransferStatus.TRANSFERRING, attempts.get(1).progress().status());
        assertEquals(List.of("pc-1"), transport.cancelled);
    }

    private List<TransferProgress> distribute(Path file, List<String> machineIds) throws IOException {
        try {
            return service.distribute(file, machineIds, TEACHER_SESSION_ID).stream()
                    .map(FileTransferAttempt::progress)
                    .toList();
        } catch (FileDistributionAuthorizationException exception) {
            throw new AssertionError("Test teacher session should be authorized", exception);
        }
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

    private static final class TestTeacherAuthorizer implements FileDistributionAuthorizer {
        private boolean authorized = true;

        @Override
        public void requireAuthorizedTeacher(String teacherSessionId)
                throws FileDistributionAuthorizationException {
            if (!authorized || !TEACHER_SESSION_ID.equals(teacherSessionId)) {
                throw new FileDistributionAuthorizationException("Teacher session is not authorized");
            }
        }
    }

    private static final class TestTransport implements FileTransferTransport {
        private final Map<String, Behavior> behaviors = new HashMap<>();
        private final java.util.ArrayList<String> started = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> cancelled = new java.util.ArrayList<>();

        @Override
        public com.dau.labmanagement.server.service.filepush.TransferHandle startTransfer(
                Path sourceFile, RecipientSession recipient, TransferProgress progress)
                throws IOException {
            started.add(recipient.machineId());
            Behavior behavior = behaviors.getOrDefault(recipient.machineId(), Behavior.COMPLETE);
            if (behavior == Behavior.FAIL) {
                progress.recordBytesSent(progress.totalBytes() / 2);
                progress.acknowledgeBytesWritten(progress.totalBytes() / 4);
                throw new IOException("simulated transport start failure");
            }

            long totalBytes = progress.totalBytes();
            long sent = behavior == Behavior.PARTIAL ? totalBytes / 2 : totalBytes;
            long acknowledged = behavior == Behavior.PARTIAL ? totalBytes / 5 : totalBytes;
            if (sent > 0) {
                progress.recordBytesSent(sent);
            }
            if (acknowledged > 0) {
                progress.acknowledgeBytesWritten(acknowledged);
            }
            if (behavior == Behavior.COMPLETE) {
                progress.complete();
            }
            return () -> cancelled.add(recipient.machineId());
        }
    }
}