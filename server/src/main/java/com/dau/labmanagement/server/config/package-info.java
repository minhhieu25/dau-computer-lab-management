/**
 * Application configuration and infrastructure bootstrap.
 *
 * <p>Planned classes (added at FND-02, Sprint 0): {@code AppConfig} reads
 * DB_URL / DB_USER / DB_PASSWORD from environment variables (never hardcode
 * credentials), {@code DataSourceProvider} builds the HikariCP pool,
 * {@code DatabaseMigrator} runs Flyway migrations from db/migration,
 * {@code JdbiProvider} creates the JDBI handle factory.</p>
 */
package com.dau.labmanagement.server.config;
