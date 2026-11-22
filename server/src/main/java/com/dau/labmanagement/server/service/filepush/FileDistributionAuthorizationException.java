package com.dau.labmanagement.server.service.filepush;

/** Indicates that the current server principal is not authorized to distribute files. */
public class FileDistributionAuthorizationException extends Exception {
    private static final long serialVersionUID = 1L;

    public FileDistributionAuthorizationException(String message) {
        super(message);
    }
}