package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.SubmissionSessionRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * DAO for the {@code submission_session} table — one "collect homework"
 * round opened by a teacher (BR-05).
 */
public interface SubmissionSessionDao {

    @SqlQuery("""
            INSERT INTO submission_session (user_id, session_name, status)
            VALUES (:userId, :sessionName, 'collecting')
            RETURNING submission_session_id""")
    long insert(@Bind("userId") long userId, @Bind("sessionName") String sessionName);

    @SqlQuery("""
            SELECT submission_session_id, user_id, session_name, started_at, ended_at, status
            FROM submission_session
            WHERE submission_session_id = :submissionSessionId""")
    Optional<SubmissionSessionRow> findById(@Bind("submissionSessionId") long submissionSessionId);

    @SqlQuery("""
            SELECT submission_session_id, user_id, session_name, started_at, ended_at, status
            FROM submission_session
            ORDER BY started_at DESC""")
    List<SubmissionSessionRow> findAll();

    @SqlUpdate("""
            UPDATE submission_session
            SET ended_at = now(), status = 'completed'
            WHERE submission_session_id = :submissionSessionId AND ended_at IS NULL""")
    int endSession(@Bind("submissionSessionId") long submissionSessionId);
}
