package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.UserRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * DAO for the {@code app_user} table (ERD entity "User").
 * Role and status values follow the CHECK constraints: see
 * {@code UserRole} and {@code AccountStatus} for the constants.
 */
public interface UserDao {

    @SqlQuery("""
            SELECT user_id, username, full_name, role, status, created_at, deleted_at
            FROM app_user
            WHERE deleted_at IS NULL
            ORDER BY username""")
    List<UserRow> findActiveUsers();

    @SqlQuery("""
            SELECT user_id, username, password_hash, full_name, role, status, created_at, deleted_at
            FROM app_user
            WHERE user_id = :userId""")
    Optional<UserRow> findById(@Bind("userId") long userId);

    @SqlQuery("""
            SELECT user_id, username, password_hash, full_name, role, status, created_at, deleted_at
            FROM app_user
            WHERE username = :username AND deleted_at IS NULL""")
    Optional<UserRow> findActiveByUsername(@Bind("username") String username);

    @SqlQuery("""
            INSERT INTO app_user (username, password_hash, full_name, role, status)
            VALUES (:username, :passwordHash, :fullName, :role, :status)
            RETURNING user_id""")
    long insert(@Bind("username") String username,
                @Bind("passwordHash") String passwordHash,
                @Bind("fullName") String fullName,
                @Bind("role") String role,
                @Bind("status") String status);

    @SqlUpdate("UPDATE app_user SET status = :status WHERE user_id = :userId")
    int updateStatus(@Bind("userId") long userId, @Bind("status") String status);

    @SqlUpdate("UPDATE app_user SET password_hash = :passwordHash WHERE user_id = :userId")
    int updatePasswordHash(@Bind("userId") long userId, @Bind("passwordHash") String passwordHash);

    @SqlUpdate("UPDATE app_user SET deleted_at = now() WHERE user_id = :userId AND deleted_at IS NULL")
    int softDelete(@Bind("userId") long userId);
}
