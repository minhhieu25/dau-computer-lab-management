package com.dau.labmanagement.server.config;

/**
 * Central configuration read from environment variables.
 * Credentials are never hardcoded (Docs/coding_convention.md, section 3).
 *
 * <p>Environment variables:</p>
 * <ul>
 *   <li>{@code DB_URL} — JDBC URL, default {@code jdbc:postgresql://localhost:5433/lab_management}</li>
 *   <li>{@code DB_USER} — database user, default {@code postgres}</li>
 *   <li>{@code DB_PASSWORD} — database password, required for database work</li>
 *   <li>{@code DB_MIGRATION_LOCATIONS} — Flyway locations, default
 *       {@code filesystem:db/migration} (relative to the repo root, which is
 *       the working directory configured for run/test Gradle tasks)</li>
 * </ul>
 */
public final class AppConfig {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String migrationLocations;

    private AppConfig(String dbUrl, String dbUser, String dbPassword, String migrationLocations) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.migrationLocations = migrationLocations;
    }

    /**
     * Reads the configuration from environment variables.
     *
     * @return the application configuration
     */
    public static AppConfig fromEnv() {
        return new AppConfig(
                envOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/lab_management"),
                envOrDefault("DB_USER", "postgres"),
                System.getenv("DB_PASSWORD"),
                envOrDefault("DB_MIGRATION_LOCATIONS", "filesystem:db/migration"));
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    /** May be null when the variable is unset; database features check this. */
    public String dbPassword() {
        return dbPassword;
    }

    public String migrationLocations() {
        return migrationLocations;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
