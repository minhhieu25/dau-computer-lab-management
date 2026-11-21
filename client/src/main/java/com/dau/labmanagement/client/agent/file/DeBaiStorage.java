package com.dau.labmanagement.client.agent.file;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Resolves and stages received assignment files inside the logged-in student's DeBai folder. */
public final class DeBaiStorage {
    public static final Path DEFAULT_SHARED_ROOT = Path.of("D:\\ThuMucDungChung");
    private static final Set<String> RESERVED_WINDOWS_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL");

    private final Path sharedRoot;
    private final CurrentStudentIdProvider studentIdProvider;

    public DeBaiStorage(CurrentStudentIdProvider studentIdProvider) {
        this(DEFAULT_SHARED_ROOT, studentIdProvider);
    }

    public DeBaiStorage(Path sharedRoot, CurrentStudentIdProvider studentIdProvider) {
        this.sharedRoot = Objects.requireNonNull(sharedRoot, "sharedRoot").toAbsolutePath().normalize();
        this.studentIdProvider = Objects.requireNonNull(studentIdProvider, "studentIdProvider");
    }

    /** Resolves a server-provided basename under the current student's DeBai folder. */
    public Path resolveDestination(String fileName) {
        return resolveDestination(currentStudentId(), fileName);
    }

    /** Checks whether a pending receive still belongs to the currently logged-in student. */
    public boolean isCurrentStudent(PendingFile pendingFile) {
        Objects.requireNonNull(pendingFile, "pendingFile");
        return studentIdProvider.currentStudentId()
                .filter(pendingFile.studentId::equals)
                .isPresent();
    }

    private Path resolveDestination(String studentId, String fileName) {
        validatePathSegment(studentId, "studentId");
        validateFileName(fileName);

        Path root = sharedRoot.normalize();
        Path destination = root.resolve(studentId).resolve("DeBai").resolve(fileName).normalize();
        if (!destination.startsWith(root)) {
            throw new IllegalArgumentException("Resolved destination escapes the shared root");
        }
        return destination;
    }

    /** Creates a new .part file and refuses to replace an existing destination or partial file. */
    public PendingFile prepare(String fileName, long expectedBytes) throws IOException {
        if (expectedBytes < 0) {
            throw new IllegalArgumentException("expectedBytes must not be negative");
        }

        String studentId = currentStudentId();
        Path destination = resolveDestination(studentId, fileName);
        Path root = ensureRoot();
        Path studentDirectory = root.resolve(studentId).normalize();
        if (!studentDirectory.startsWith(root) || Files.isSymbolicLink(studentDirectory)) {
            throw new IOException("Student directory is not a safe path under the shared root");
        }
        Files.createDirectories(studentDirectory);
        Path realStudentDirectory = studentDirectory.toRealPath();
        if (!realStudentDirectory.startsWith(root)) {
            throw new IOException("Student directory resolves outside the shared root");
        }

        Path directory = realStudentDirectory.resolve("DeBai");
        if (Files.isSymbolicLink(directory)) {
            throw new IOException("Assignment directory must not be a symbolic link");
        }
        Files.createDirectories(directory);
        Path realDirectory = directory.toRealPath();
        if (!realDirectory.startsWith(root)) {
            throw new IOException("Assignment directory resolves outside the shared root");
        }

        Path resolvedDestination = realDirectory.resolve(destination.getFileName());
        if (Files.exists(resolvedDestination)) {
            throw new FileAlreadyExistsException(resolvedDestination.toString());
        }
        Path temporaryFile = realDirectory.resolve(destination.getFileName() + ".part");
        Files.createFile(temporaryFile);
        return new PendingFile(temporaryFile, resolvedDestination, expectedBytes, studentId, studentIdProvider);
    }

    private String currentStudentId() {
        return studentIdProvider.currentStudentId()
                .map(value -> requireText(value, "studentId"))
                .orElseThrow(() -> new IllegalStateException("A logged-in student is required to receive files"));
    }

    private Path ensureRoot() throws IOException {
        Files.createDirectories(sharedRoot);
        return sharedRoot.toRealPath();
    }

    private static void validateFileName(String fileName) {
        String value = requireText(fileName, "fileName");
        validatePathSegment(value, "fileName");
        if (value.endsWith(".") || value.endsWith(" ")) {
            throw new IllegalArgumentException("fileName must not end with a dot or space");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 32 || "<>:\"|?*".indexOf(character) >= 0) {
                throw new IllegalArgumentException("fileName contains a character not allowed in a Windows path");
            }
        }
    }

    private static void validatePathSegment(String value, String fieldName) {
        if (value.equals(".") || value.equals("..") || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(fieldName + " must be a single path segment");
        }
        if (value.endsWith(".") || value.endsWith(" ")) {
            throw new IllegalArgumentException(fieldName + " must not end with a dot or space");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 32 || "<>:\"|?*".indexOf(character) >= 0) {
                throw new IllegalArgumentException(fieldName + " contains a character not allowed in a Windows path");
            }
        }
        String baseName = value.split("\\.", 2)[0].stripTrailing().toUpperCase(Locale.ROOT);
        if (RESERVED_WINDOWS_NAMES.contains(baseName)
                || baseName.matches("COM[1-9]")
                || baseName.matches("LPT[1-9]")) {
            throw new IllegalArgumentException(fieldName + " must not be a reserved Windows device name");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public static final class PendingFile {
        private final Path temporaryPath;
        private final Path destinationPath;
        private final long expectedBytes;
        private final String studentId;
        private final CurrentStudentIdProvider studentIdProvider;
        private boolean finished;

        private PendingFile(
                Path temporaryPath,
                Path destinationPath,
                long expectedBytes,
                String studentId,
                CurrentStudentIdProvider studentIdProvider) {
            this.temporaryPath = temporaryPath;
            this.destinationPath = destinationPath;
            this.expectedBytes = expectedBytes;
            this.studentId = studentId;
            this.studentIdProvider = studentIdProvider;
        }

        public Path temporaryPath() {
            return temporaryPath;
        }

        public long expectedBytes() {
            return expectedBytes;
        }

        /** Promotes the partial file only when the received and on-disk sizes match the header. */
        public synchronized Path complete(long receivedBytes) throws IOException {
            requireOpen();
            try {
                if (studentIdProvider.currentStudentId().filter(studentId::equals).isEmpty()) {
                    throw new IOException("Student session changed before file completion");
                }
                if (receivedBytes != expectedBytes || Files.size(temporaryPath) != expectedBytes) {
                    throw new IOException("Received file size does not match the declared size");
                }
                Files.move(temporaryPath, destinationPath);
                finished = true;
                return destinationPath;
            } catch (IOException exception) {
                cleanupAfterFailure(exception);
                throw exception;
            }
        }

        /** Removes the partial file after an interrupted or failed receive. */
        public synchronized void fail() throws IOException {
            if (finished) {
                return;
            }
            Files.deleteIfExists(temporaryPath);
            finished = true;
        }

        private void cleanupAfterFailure(IOException failure) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            finished = true;
        }

        private void requireOpen() {
            if (finished) {
                throw new IllegalStateException("Pending file is already finalized");
            }
        }
    }
}