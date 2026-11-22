package com.dau.labmanagement.server.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for FND-02: runs the real V1/V2 migrations on an
 * embedded PostgreSQL (Zonky — no Docker, no external database) and asserts
 * the schema contract: 9 tables, all FK constraints, all basic indexes and
 * the seed admin account.
 */
class DatabaseMigrationIT {

    private static final List<String> ALL_TABLES = List.of(
            "app_user", "machine", "system_setting", "student_session", "submission_session",
            "recording_file", "recording_log", "submission", "submission_file");

    /** FK constraints in V1__schema.sql. */
    private static final int EXPECTED_FOREIGN_KEYS = 12;

    /** Explicitly created idx_% indexes in V1__schema.sql. */
    private static final int EXPECTED_INDEXES = 13;

    @Test
    void migrationsCreateAllTablesConstraintsAndSeed() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            DataSource dataSource = postgres.getPostgresDatabase();

            assertEquals(2, DatabaseMigrator.migrate(dataSource, "classpath:db/migration"),
                    "V1__schema.sql and V2__seed.sql must both apply");
            assertEquals(0, DatabaseMigrator.migrate(dataSource, "classpath:db/migration"),
                    "re-running migrations must be a no-op (Flyway idempotency)");

            try (Connection connection = dataSource.getConnection()) {
                assertTablesExist(connection);
                assertConstraintCounts(connection);
                assertSeedAdmin(connection);
                assertForeignKeyRule(connection);
            }
        }
    }

    private static void assertTablesExist(Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, "public", "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        assertTrue(tables.containsAll(ALL_TABLES),
                "missing tables, found only: " + tables);
    }

    private static void assertConstraintCounts(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT count(*) FROM information_schema.table_constraints "
                    + "WHERE constraint_type = 'FOREIGN KEY' AND constraint_name LIKE 'fk_%'")) {
                rs.next();
                assertEquals(EXPECTED_FOREIGN_KEYS, rs.getInt(1), "foreign key constraints");
            }
            try (ResultSet rs = statement.executeQuery(
                    "SELECT count(*) FROM pg_indexes WHERE indexname LIKE 'idx_%'")) {
                rs.next();
                assertEquals(EXPECTED_INDEXES, rs.getInt(1), "basic indexes");
            }
        }
    }

    private static void assertSeedAdmin(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT count(*) FROM app_user "
                     + "WHERE username = 'admin' AND role = 'admin' AND status = 'active' "
                     + "AND deleted_at IS NULL AND password_hash LIKE 'pbkdf2-sha256$%'")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "seed admin account");
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT retention_days FROM system_setting WHERE system_setting_id = 1")) {
            rs.next();
            assertEquals(30, rs.getInt(1), "default retention days (BR-10)");
        }
    }

    private static void assertForeignKeyRule(Connection connection) throws Exception {
        // Prove the FK graph works: inserting a recording_log for a non-existent
        // recording must violate the constraint.
        try (Statement statement = connection.createStatement()) {
            try {
                statement.execute(
                        "INSERT INTO recording_log (user_id, recording_file_id, action) "
                        + "VALUES (1, 999999, 'view')");
                throw new AssertionError("FK fk_recording_log_recording_file_id did not fire");
            } catch (Exception expected) {
                assertTrue(expected.getMessage() != null
                                && expected.getMessage().contains("fk_recording_log_recording_file_id"),
                        "expected FK violation, got: " + expected.getMessage());
            }
        }
    }
}
