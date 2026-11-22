package com.dau.labmanagement.server.model.entity;

/**
 * Row record for the {@code submission_file} table — one collected file
 * on the server.
 *
 * @param submissionFileId primary key
 * @param submissionId     the owning submission
 * @param fileName         original file name on the client
 * @param filePath         server path: ThuBai\&lt;teacher\&gt;\&lt;datetime\&gt;\&lt;IP\&gt;_\&lt;MSSV\&gt;\
 * @param fileSizeBytes    file size in bytes
 */
public record SubmissionFileRow(
        Long submissionFileId,
        Long submissionId,
        String fileName,
        String filePath,
        Long fileSizeBytes) {
}
