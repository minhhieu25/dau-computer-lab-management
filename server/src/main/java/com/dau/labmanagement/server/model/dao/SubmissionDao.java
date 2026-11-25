package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.SubmissionRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * DAO for the {@code submission} table (BR-05). A submission starts as
 * 'submitting'; a dropped connection keeps that state so the transfer can resume.
 */
public interface SubmissionDao {

    @SqlQuery("""
            INSERT INTO submission (submission_session_id, student_user_id, machine_id, status)
            VALUES (:submissionSessionId, :studentUserId, :machineId, 'submitting')
            RETURNING submission_id""")
    long insert(@Bind("submissionSessionId") long submissionSessionId,
                @Bind("studentUserId") long studentUserId,
                @Bind("machineId") long machineId);

    @SqlQuery("""
            SELECT submission_id, submission_session_id, student_user_id, machine_id, status, submitted_at
            FROM submission
            WHERE submission_id = :submissionId""")
    Optional<SubmissionRow> findById(@Bind("submissionId") long submissionId);

    @SqlQuery("""
            SELECT submission_id, submission_session_id, student_user_id, machine_id, status, submitted_at
            FROM submission
            WHERE submission_session_id = :submissionSessionId
            ORDER BY submitted_at NULLS LAST, submission_id""")
    List<SubmissionRow> findBySession(@Bind("submissionSessionId") long submissionSessionId);

    @SqlUpdate("UPDATE submission SET status = :status WHERE submission_id = :submissionId")
    int updateStatus(@Bind("submissionId") long submissionId, @Bind("status") String status);

    @SqlUpdate("""
            UPDATE submission
            SET status = 'submitted', submitted_at = now()
            WHERE submission_id = :submissionId""")
    int markSubmitted(@Bind("submissionId") long submissionId);
}
