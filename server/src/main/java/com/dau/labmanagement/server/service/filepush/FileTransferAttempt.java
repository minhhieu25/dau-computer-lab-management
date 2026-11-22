package com.dau.labmanagement.server.service.filepush;

import com.dau.labmanagement.server.model.state.TransferProgress;
import com.dau.labmanagement.server.model.state.TransferStatus;
import java.io.IOException;
import java.util.Objects;

/** Progress and cancellation control for one selected recipient. */
public final class FileTransferAttempt {
    private final TransferProgress progress;
    private final TransferHandle handle;

    FileTransferAttempt(TransferProgress progress, TransferHandle handle) {
        this.progress = Objects.requireNonNull(progress, "progress");
        this.handle = handle;
    }

    public TransferProgress progress() {
        return progress;
    }

    /** Cancels only this recipient's active transfer. */
    public synchronized boolean cancel() {
        TransferStatus status = progress.status();
        if (status != TransferStatus.PENDING && status != TransferStatus.TRANSFERRING) {
            return false;
        }
        if (handle == null) {
            progress.fail("CANCELLATION_UNAVAILABLE");
            return false;
        }
        try {
            handle.cancel();
            progress.cancel("CANCELLED_BY_TEACHER");
            return true;
        } catch (IOException | RuntimeException exception) {
            progress.fail("CANCELLATION_FAILED");
            return false;
        }
    }
}