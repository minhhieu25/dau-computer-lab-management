package com.dau.labmanagement.client;

import com.dau.labmanagement.client.agent.file.DeBaiStorage;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeBaiStorageTest {
    private static final String STUDENT_ID = "20260001";

    @TempDir
    Path temporaryDirectory;

    private DeBaiStorage storage;

    @BeforeEach
    void setUp() {
        storage = new DeBaiStorage(temporaryDirectory, () -> Optional.of(STUDENT_ID));
    }

    @Test
    void buildsDestinationFromCurrentAuthenticatedStudentId() {
        Path destination = storage.resolveDestination("assignment.zip");

        assertEquals(temporaryDirectory.resolve(STUDENT_ID).resolve("DeBai").resolve("assignment.zip"),
                destination);
    }

    @Test
    void requiresLoggedInStudentAndRejectsTraversal() {
        DeBaiStorage loggedOutStorage = new DeBaiStorage(temporaryDirectory, Optional::empty);

        assertThrows(IllegalStateException.class, () -> loggedOutStorage.prepare("assignment.zip", 1));
        assertThrows(IllegalArgumentException.class, () -> storage.resolveDestination("../outside.txt"));
        DeBaiStorage traversalStudent = new DeBaiStorage(temporaryDirectory, () -> Optional.of("../other"));
        assertThrows(IllegalArgumentException.class, () -> traversalStudent.resolveDestination("file.bin"));
        DeBaiStorage driveRelativeStudent = new DeBaiStorage(temporaryDirectory, () -> Optional.of("D:"));
        assertThrows(IllegalArgumentException.class, () -> driveRelativeStudent.resolveDestination("file.bin"));
    }

    @Test
    void refusesToOverwriteExistingFileOrPartialFile() throws IOException {
        Path destination = storage.resolveDestination("assignment.zip");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "existing");
        assertThrows(FileAlreadyExistsException.class, () -> storage.prepare("assignment.zip", 8));

        Files.delete(destination);
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        Files.createFile(partial);
        assertThrows(FileAlreadyExistsException.class, () -> storage.prepare("assignment.zip", 8));
    }

    @Test
    void promotesOnlyCompleteFileWithoutChangingBytes() throws IOException {
        byte[] sourceBytes = {0, 1, 2, 13, 10, -1};
        DeBaiStorage.PendingFile pendingFile = storage.prepare("assignment.bin", sourceBytes.length);
        Files.write(pendingFile.temporaryPath(), sourceBytes);

        Path destination = pendingFile.complete(sourceBytes.length);

        assertArrayEquals(sourceBytes, Files.readAllBytes(destination));
        assertFalse(Files.exists(pendingFile.temporaryPath()));
    }

    @Test
    void incompleteReceiveDeletesPartAndDoesNotCreateFinalFile() throws IOException {
        DeBaiStorage.PendingFile pendingFile = storage.prepare("assignment.bin", 5);
        Files.write(pendingFile.temporaryPath(), new byte[]{1, 2});

        assertThrows(IOException.class, () -> pendingFile.complete(2));

        assertFalse(Files.exists(pendingFile.temporaryPath()));
        assertFalse(Files.exists(storage.resolveDestination("assignment.bin")));
    }

    @Test
    void explicitReceiveFailureDeletesPart() throws IOException {
        DeBaiStorage.PendingFile pendingFile = storage.prepare("assignment.bin", 10);

        pendingFile.fail();

        assertFalse(Files.exists(pendingFile.temporaryPath()));
        assertTrue(Files.exists(pendingFile.temporaryPath().getParent()));
    }

    @Test
    void emptyFileCanBeCompleted() throws IOException {
        DeBaiStorage.PendingFile pendingFile = storage.prepare("empty.zip", 0);

        Path destination = pendingFile.complete(0);

        assertEquals(0, Files.size(destination));
    }
}