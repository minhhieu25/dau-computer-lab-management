package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code system_setting} singleton table.
 *
 * @param systemSettingId always 1 (enforced by a CHECK constraint)
 * @param retentionDays   recording retention in days, 1..120 (BR-10)
 * @param updatedBy       the admin who last changed the settings
 * @param updatedAt       last change time
 */
public record SystemSettingRow(
        Integer systemSettingId,
        Integer retentionDays,
        Long updatedBy,
        OffsetDateTime updatedAt) {
}
