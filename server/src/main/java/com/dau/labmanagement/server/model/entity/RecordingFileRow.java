package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code recording_file} table — screen recordings,
 * stored only on the server (BR-07).
 *
 * @param recordingFileId  primary key
 * @param userId           the teacher who recorded
 * @param studentSessionId the monitored student session
 * @param fileName         original file name
 * @param filePath         absolute path on the server (unique)
 * @param startedAt        recording start
 * @param endedAt          recording end; null while recording
 * @param fileSizeBytes    file size in bytes
 * @param status           recording | stored | deleted (see {@link RecordingFileStatus})
 * @param expiresAt        the retention job deletes after this moment (BR-10)
 */
public record RecordingFileRow(
        Long recordingFileId,
        Long userId,
        Long studentSessionId,
        String fileName,
        String filePath,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long fileSizeBytes,
        String status,
        OffsetDateTime expiresAt) {
}
