package com.dau.labmanagement.server.model.state;

/** States in the lifecycle of one client file transfer. */
public enum TransferStatus {
    PENDING,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED
}