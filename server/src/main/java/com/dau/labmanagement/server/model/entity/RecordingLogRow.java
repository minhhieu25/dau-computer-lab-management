package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code recording_log} table — audit log of sensitive
 * actions on recordings (BR-10).
 *
 * @param recordingLogId  primary key
 * @param userId          who performed the action
 * @param recordingFileId the affected recording
 * @param action          start | stop | view | delete | set_retention ...
 * @param createdAt       when the action happened
 */
public record RecordingLogRow(
        Long recordingLogId,
        Long userId,
        Long recordingFileId,
        String action,
        OffsetDateTime createdAt) {
}
