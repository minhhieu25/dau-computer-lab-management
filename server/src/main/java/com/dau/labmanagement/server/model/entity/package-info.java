/**
 * MODEL layer — row records mapping the 9 database tables one-to-one.
 *
 * <p>The same mapping convention applies to every record here:
 * <ul>
 *   <li>snake_case columns map to camelCase components through JDBI's
 *       {@code SnakeCaseColumnNameMatcher}, configured once in
 *       {@code config.JdbiProvider};</li>
 *   <li>TIMESTAMPTZ maps to {@code java.time.OffsetDateTime}, UUID to
 *       {@code java.util.UUID}, NUMERIC to {@code java.math.BigDecimal};</li>
 *   <li>columns guarded by CHECK constraints (role, status) stay {@code String}
 *       inside records and are converted through the enums of this package
 *       ({@code fromDb} / {@code toDb}) at the service layer.</li>
 * </ul>
 * ERD note: table {@code app_user} is the ERD entity "User" — {@code user} is
 * a reserved keyword in PostgreSQL.</p>
 */
package com.dau.labmanagement.server.model.entity;
