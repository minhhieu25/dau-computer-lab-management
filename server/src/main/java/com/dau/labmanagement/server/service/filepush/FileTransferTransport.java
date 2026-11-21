package com.dau.labmanagement.server.service.filepush;

import com.dau.labmanagement.server.model.state.TransferProgress;
import java.io.IOException;
import java.nio.file.Path;

/** Adapter boundary for a future approved file-transfer transport such as FND-03 TCP. */
public interface FileTransferTransport {
    /**
     * Starts one transfer attempt and returns without waiting for unrelated targets.
     * Implementations update progress from transport writes and receiver acknowledgements.
     * @param sourceFile the server-side source file
     * @param recipient the validated server-side recipient session
     * @param progress the progress model belonging only to this recipient
     * @throws IOException if the attempt cannot be started
     */
    void startTransfer(Path sourceFile, RecipientSession recipient, TransferProgress progress) throws IOException;
}