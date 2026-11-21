package com.dau.labmanagement.server.service.filepush;

/** Indicates that authoritative recipient session state could not be read. */
public class RecipientLookupException extends Exception {
    private static final long serialVersionUID = 1L;

    public RecipientLookupException(String message) {
        super(message);
    }

    public RecipientLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}