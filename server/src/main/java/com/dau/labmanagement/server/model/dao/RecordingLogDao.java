package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.RecordingLogRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * DAO for the {@code recording_log} table — audit log of sensitive actions (BR-10).
 */
public interface RecordingLogDao {

    @SqlQuery("""
            INSERT INTO recording_log (user_id, recording_file_id, action)
            VALUES (:userId, :recordingFileId, :action)
            RETURNING recording_log_id""")
    long insert(@Bind("userId") long userId,
                @Bind("recordingFileId") long recordingFileId,
                @Bind("action") String action);

    @SqlQuery("""
            SELECT recording_log_id, user_id, recording_file_id, action, created_at
            FROM recording_log
            WHERE recording_file_id = :recordingFileId
            ORDER BY created_at DESC""")
    List<RecordingLogRow> findByRecordingFile(@Bind("recordingFileId") long recordingFileId);
}
