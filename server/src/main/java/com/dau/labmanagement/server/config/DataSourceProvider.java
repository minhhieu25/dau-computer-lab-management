package com.dau.labmanagement.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Builds the HikariCP connection pool used by the whole server.
 * The pool is created once at startup and lives for the process lifetime.
 */
public final class DataSourceProvider {

    private DataSourceProvider() {
        // Utility class, no instances.
    }

    /**
     * Creates a connection pool from the given configuration.
     *
     * @param config the application configuration
     * @return a ready-to-use Hikari data source
     * @throws IllegalStateException if DB_PASSWORD is not set
     */
    public static HikariDataSource create(AppConfig config) {
        if (config.dbPassword() == null || config.dbPassword().isBlank()) {
            throw new IllegalStateException(
                    "DB_PASSWORD environment variable is not set; database features are unavailable");
        }
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.dbUrl());
        hikari.setUsername(config.dbUser());
        hikari.setPassword(config.dbPassword());
        hikari.setMaximumPoolSize(10);
        hikari.setPoolName("lab-management-pool");
        return new HikariDataSource(hikari);
    }
}
