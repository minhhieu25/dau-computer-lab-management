package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.StudentSessionRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the {@code student_session} table — one student logged in on one machine.
 */
public interface StudentSessionDao {

    @SqlQuery("""
            INSERT INTO student_session (user_id, machine_id)
            VALUES (:userId, :machineId)
            RETURNING student_session_id""")
    long insert(@Bind("userId") long userId, @Bind("machineId") long machineId);

    @SqlQuery("""
            SELECT student_session_id, user_id, machine_id, login_at, logout_at
            FROM student_session
            WHERE student_session_id = :studentSessionId""")
    Optional<StudentSessionRow> findById(@Bind("studentSessionId") long studentSessionId);

    @SqlQuery("""
            SELECT student_session_id, user_id, machine_id, login_at, logout_at
            FROM student_session
            WHERE user_id = :userId
            ORDER BY login_at DESC""")
    List<StudentSessionRow> findByUser(@Bind("userId") long userId);

    @SqlUpdate("""
            UPDATE student_session
            SET logout_at = :logoutAt
            WHERE student_session_id = :studentSessionId AND logout_at IS NULL""")
    int closeSession(@Bind("studentSessionId") long studentSessionId, @Bind("logoutAt") OffsetDateTime logoutAt);
}
