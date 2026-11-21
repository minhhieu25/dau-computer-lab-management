package com.dau.labmanagement.client.model.state;

import com.dau.labmanagement.client.model.entity.StudentIdentity;
import java.util.Objects;
import java.util.Optional;

/** Keeps network presence independent from the student's authenticated identity. */
public final class ClientSessionState {
    private boolean online;
    private StudentIdentity studentIdentity;

    /** Returns whether the client has a network connection to the server. */
    public synchronized boolean isOnline() {
        return online;
    }

    /** Marks the client as connected without requiring a student login. */
    public synchronized void markOnline() {
        online = true;
    }

    /** Marks the client as disconnected without changing the saved identity. */
    public synchronized void markOffline() {
        online = false;
    }

    /**
     * Returns the current student identity, if one is authenticated.
     * @return the current identity or an empty optional
     */
    public synchronized Optional<StudentIdentity> studentIdentity() {
        return Optional.ofNullable(studentIdentity);
    }

    /**
     * Attaches an identity if this client does not already have one.
     * @param identity the authenticated student identity
     * @return true if the identity was attached
     */
    public synchronized boolean attachStudentIdentity(StudentIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        if (studentIdentity != null) {
            return false;
        }
        studentIdentity = identity;
        return true;
    }

    /** Removes the student identity without changing network presence. */
    public synchronized void clearStudentIdentity() {
        studentIdentity = null;
    }
}