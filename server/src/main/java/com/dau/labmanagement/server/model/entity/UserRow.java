package com.dau.labmanagement.server.model.entity;

import java.time.OffsetDateTime;

/**
 * Row record for the {@code app_user} table (ERD entity "User").
 *
 * @param userId       primary key
 * @param username     unique login name
 * @param passwordHash PBKDF2 hash, see V2__seed.sql for the format
 * @param fullName     display name
 * @param role         admin | teacher | student (see {@link UserRole})
 * @param status       active | locked (see {@link AccountStatus})
 * @param createdAt    row creation time
 * @param deletedAt    soft-delete marker (BR-09); null while active
 */
public record UserRow(
        Long userId,
        String username,
        String passwordHash,
        String fullName,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt) {
}
