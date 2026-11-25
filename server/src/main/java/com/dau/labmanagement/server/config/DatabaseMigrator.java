package com.dau.labmanagement.server.config;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Runs Flyway migrations from the server classpath at startup.
 * Migrations are versioned V1__, V2__, ... and applied exactly once each.
 */
public final class DatabaseMigrator {

    private DatabaseMigrator() {
        // Utility class, no instances.
    }

    /**
     * Applies all pending migrations.
     *
     * @param dataSource the target database
     * @param locations  comma-separated Flyway locations, e.g. "classpath:db/migration"
     * @return the number of migrations applied by this call (0 when up to date)
     */
    public static int migrate(DataSource dataSource, String locations) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations.split(","))
                .load();
        return flyway.migrate().migrationsExecuted;
    }
}
