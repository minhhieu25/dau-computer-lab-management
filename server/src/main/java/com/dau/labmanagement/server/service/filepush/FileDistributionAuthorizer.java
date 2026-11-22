package com.dau.labmanagement.server.service.filepush;

/** Validates the current server-side teacher session allowed to distribute assignment files. */
@FunctionalInterface
public interface FileDistributionAuthorizer {
    /**
     * Requires an authenticated, authorized teacher session.
     * @param teacherSessionId session identifier from the server authentication context
     * @throws FileDistributionAuthorizationException if the session is missing, invalid, or not a teacher
     */
    void requireAuthorizedTeacher(String teacherSessionId) throws FileDistributionAuthorizationException;
}