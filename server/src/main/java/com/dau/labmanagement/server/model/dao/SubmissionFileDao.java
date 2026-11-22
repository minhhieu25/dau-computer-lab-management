package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.SubmissionFileRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

/**
 * DAO for the {@code submission_file} table — one collected file on the server (BR-05).
 */
public interface SubmissionFileDao {

    @SqlQuery("""
            INSERT INTO submission_file (submission_id, file_name, file_path, file_size_bytes)
            VALUES (:submissionId, :fileName, :filePath, :fileSizeBytes)
            RETURNING submission_file_id""")
    long insert(@Bind("submissionId") long submissionId,
                @Bind("fileName") String fileName,
                @Bind("filePath") String filePath,
                @Bind("fileSizeBytes") long fileSizeBytes);

    @SqlQuery("""
            SELECT submission_file_id, submission_id, file_name, file_path, file_size_bytes
            FROM submission_file
            WHERE submission_id = :submissionId
            ORDER BY submission_file_id""")
    List<SubmissionFileRow> findBySubmission(@Bind("submissionId") long submissionId);
}
