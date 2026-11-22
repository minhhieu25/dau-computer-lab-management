package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code student_session} table — a student logged in
 * on one machine.
 *
 * @param studentSessionId primary key
 * @param userId           the student's account
 * @param machineId        the machine used
 * @param loginAt          when the student logged in
 * @param logoutAt         when the student logged out; null while active
 */
public record StudentSessionRow(
        Long studentSessionId,
        Long userId,
        Long machineId,
        OffsetDateTime loginAt,
        OffsetDateTime logoutAt) {
}
