package com.dau.labmanagement.server.service.filepush;

import java.util.Optional;

/** Provides server-authoritative connection and student-session data for a machine. */
public interface RecipientSessionDirectory {
    /**
     * Finds the current server-side session for a machine.
     * @param machineId the selected machine identifier
     * @return the current session, or empty when the machine is unknown
     * @throws RecipientLookupException if the authoritative session source is unavailable
     */
    Optional<RecipientSession> findByMachineId(String machineId) throws RecipientLookupException;
}