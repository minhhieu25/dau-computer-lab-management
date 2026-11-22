package com.dau.labmanagement.server.service.filepush;

import java.io.IOException;

/** Cancels one transport transfer without affecting other recipients. */
@FunctionalInterface
public interface TransferHandle {
    /** Requests cancellation of this transfer. */
    void cancel() throws IOException;
}