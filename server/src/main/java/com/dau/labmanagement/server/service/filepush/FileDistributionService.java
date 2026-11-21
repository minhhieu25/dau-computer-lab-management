package com.dau.labmanagement.server.service.filepush;

import com.dau.labmanagement.server.model.state.TransferProgress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Validates selected recipients and starts independent file transfers through an adapter. */
public final class FileDistributionService {
    private final RecipientSessionDirectory recipientDirectory;
    private final FileTransferTransport transport;

    public FileDistributionService(RecipientSessionDirectory recipientDirectory, FileTransferTransport transport) {
        this.recipientDirectory = Objects.requireNonNull(recipientDirectory, "recipientDirectory");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Starts an independent transfer attempt for each distinct selected machine.
     * @param sourceFile the selected source file
     * @param machineIds selected machine identifiers, never student IDs from a UI form
     * @return one progress item per distinct machine in selection order
     * @throws IOException if the source file is missing, unreadable, or not a regular file
     */
    public List<TransferProgress> distribute(Path sourceFile, List<String> machineIds) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(machineIds, "machineIds");
        if (machineIds.isEmpty()) {
            throw new IllegalArgumentException("At least one machine must be selected");
        }

        LinkedHashSet<String> uniqueMachineIds = new LinkedHashSet<>();
        for (String machineId : machineIds) {
            uniqueMachineIds.add(requireText(machineId, "machineId"));
        }

        if (!Files.isRegularFile(sourceFile) || !Files.isReadable(sourceFile)) {
            throw new IOException("Source file must be a readable regular file");
        }
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        String fileName = normalizedSource.getFileName().toString();
        long totalBytes = Files.size(normalizedSource);
        List<TransferProgress> results = new ArrayList<>(uniqueMachineIds.size());

        for (String machineId : uniqueMachineIds) {
            results.add(startForMachine(normalizedSource, machineId, fileName, totalBytes));
        }
        return List.copyOf(results);
    }

    private TransferProgress startForMachine(Path sourceFile, String machineId, String fileName, long totalBytes) {
        Optional<RecipientSession> session;
        try {
            session = recipientDirectory.findByMachineId(machineId);
        } catch (RecipientLookupException | RuntimeException exception) {
            TransferProgress progress = newProgress(machineId, Optional.empty(), fileName, totalBytes);
            progress.fail("RECIPIENT_LOOKUP_FAILED");
            return progress;
        }

        if (session == null || session.isEmpty()) {
            TransferProgress progress = newProgress(machineId, Optional.empty(), fileName, totalBytes);
            progress.reject("MACHINE_NOT_FOUND");
            return progress;
        }

        RecipientSession recipient = session.get();
        Optional<String> studentId = recipient.studentAuthenticated() ? recipient.studentId() : Optional.empty();
        TransferProgress progress = newProgress(machineId, studentId, fileName, totalBytes);

        if (!recipient.machineId().equals(machineId)) {
            progress.reject("MACHINE_ID_MISMATCH");
        } else if (!recipient.online()) {
            progress.reject("CLIENT_OFFLINE");
        } else if (!recipient.studentAuthenticated()) {
            progress.reject("STUDENT_NOT_LOGGED_IN");
        } else if (recipient.studentId().isEmpty() || recipient.sessionId().isEmpty()
                || !recipient.sessionIdValid()) {
            progress.reject("INVALID_STUDENT_SESSION");
        } else {
            progress.start();
            try {
                transport.startTransfer(sourceFile, recipient, progress);
            } catch (IOException | RuntimeException exception) {
                progress.fail("TRANSFER_START_FAILED");
            }
        }
        return progress;
    }

    private static TransferProgress newProgress(
            String machineId, Optional<String> studentId, String fileName, long totalBytes) {
        return new TransferProgress(UUID.randomUUID(), machineId, studentId, fileName, totalBytes);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}