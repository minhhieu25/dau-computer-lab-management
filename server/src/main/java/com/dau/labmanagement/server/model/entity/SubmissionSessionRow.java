package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code submission_session} table — one "collect
 * homework" round opened by a teacher (BR-05).
 *
 * @param submissionSessionId primary key
 * @param userId              the teacher who opened the session
 * @param sessionName         descriptive name, e.g. "BT chuong 3 - lop 21A"
 * @param startedAt           when collection started
 * @param endedAt             when collection ended; null while collecting
 * @param status              collecting | completed (see {@link SubmissionSessionStatus})
 */
public record SubmissionSessionRow(
        Long submissionSessionId,
        Long userId,
        String sessionName,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String status) {
}
