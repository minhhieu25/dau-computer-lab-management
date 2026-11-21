package com.dau.labmanagement.client.agent.file;

import java.util.Optional;

/** Supplies the student ID from the currently authenticated client session. */
@FunctionalInterface
public interface CurrentStudentIdProvider {
    Optional<String> currentStudentId();
}