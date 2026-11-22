package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code submission} table — one student's homework
 * inside a collection round (BR-05).
 *
 * @param submissionId         primary key
 * @param submissionSessionId  the collection round
 * @param studentUserId        the student's account
 * @param machineId            the machine the student used
 * @param status               submitting | submitted | failed (see {@link SubmissionStatus})
 * @param submittedAt          when the transfer completed; null while submitting
 */
public record SubmissionRow(
        Long submissionId,
        Long submissionSessionId,
        Long studentUserId,
        Long machineId,
        String status,
        OffsetDateTime submittedAt) {
}
