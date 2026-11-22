package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.SystemSettingRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

/**
 * DAO for the {@code system_setting} singleton table (row 1, created by V2__seed.sql).
 */
public interface SystemSettingDao {

    @SqlQuery("""
            SELECT system_setting_id, retention_days, updated_by, updated_at
            FROM system_setting
            WHERE system_setting_id = 1""")
    Optional<SystemSettingRow> findSettings();

    @SqlUpdate("""
            UPDATE system_setting
            SET retention_days = :retentionDays, updated_by = :updatedBy, updated_at = now()
            WHERE system_setting_id = 1""")
    int updateRetention(@Bind("retentionDays") int retentionDays, @Bind("updatedBy") long updatedBy);
}
