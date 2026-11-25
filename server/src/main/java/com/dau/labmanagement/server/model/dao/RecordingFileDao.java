package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.RecordingFileRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the {@code recording_file} table (BR-07, BR-10): recordings are
 * stored only on the server; teachers see their own files, admins see all.
 */
public interface RecordingFileDao {

    @SqlQuery("""
            INSERT INTO recording_file (user_id, student_session_id, file_name, file_path, started_at, status, expires_at)
            VALUES (:userId, :studentSessionId, :fileName, :filePath, :startedAt, 'recording', :expiresAt)
            RETURNING recording_file_id""")
    long insert(@Bind("userId") long userId,
                @Bind("studentSessionId") long studentSessionId,
                @Bind("fileName") String fileName,
                @Bind("filePath") String filePath,
                @Bind("startedAt") OffsetDateTime startedAt,
                @Bind("expiresAt") OffsetDateTime expiresAt);

    @SqlQuery("""
            SELECT recording_file_id, user_id, student_session_id, file_name, file_path,
                   started_at, ended_at, file_size_bytes, status, expires_at
            FROM recording_file
            WHERE recording_file_id = :recordingFileId""")
    Optional<RecordingFileRow> findById(@Bind("recordingFileId") long recordingFileId);

    @SqlQuery("""
            SELECT recording_file_id, user_id, student_session_id, file_name, file_path,
                   started_at, ended_at, file_size_bytes, status, expires_at
            FROM recording_file
            ORDER BY started_at DESC""")
    List<RecordingFileRow> findAll();

    @SqlQuery("""
            SELECT recording_file_id, user_id, student_session_id, file_name, file_path,
                   started_at, ended_at, file_size_bytes, status, expires_at
            FROM recording_file
            WHERE user_id = :userId
            ORDER BY started_at DESC""")
    List<RecordingFileRow> findByTeacher(@Bind("userId") long userId);

    @SqlQuery("""
            SELECT recording_file_id, user_id, student_session_id, file_name, file_path,
                   started_at, ended_at, file_size_bytes, status, expires_at
            FROM recording_file
            WHERE expires_at <= :now AND status <> 'deleted'""")
    List<RecordingFileRow> findExpired(@Bind("now") OffsetDateTime now);

    @SqlUpdate("""
            UPDATE recording_file
            SET ended_at = :endedAt, file_size_bytes = :fileSizeBytes, status = 'stored'
            WHERE recording_file_id = :recordingFileId""")
    int finishRecording(@Bind("recordingFileId") long recordingFileId,
                        @Bind("endedAt") OffsetDateTime endedAt,
                        @Bind("fileSizeBytes") Long fileSizeBytes);

    @SqlUpdate("UPDATE recording_file SET status = :status WHERE recording_file_id = :recordingFileId")
    int updateStatus(@Bind("recordingFileId") long recordingFileId, @Bind("status") String status);

    @SqlUpdate("""
            UPDATE recording_file
            SET expires_at = :expiresAt
            WHERE recording_file_id = :recordingFileId""")
    int updateExpiresAt(@Bind("recordingFileId") long recordingFileId, @Bind("expiresAt") OffsetDateTime expiresAt);
}
