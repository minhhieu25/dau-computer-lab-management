package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginResultTest {

    @Test
    void successCarriesTheSessionAndNoMessage() {
        LoginSession session = new LoginSession(
                UUID.randomUUID(), 1L, "admin", "Admin", UserRole.ADMIN, OffsetDateTime.now());

        LoginResult result = LoginResult.success(session);

        assertTrue(result.isSuccess());
        assertEquals(session, result.session());
        assertEquals("", result.failureMessage());
    }

    @Test
    void rejectionCarriesTheReasonAndItsMessage() {
        LoginResult result = LoginResult.rejected(LoginFailureReason.ACCOUNT_LOCKED);

        assertFalse(result.isSuccess());
        assertEquals(LoginFailureReason.ACCOUNT_LOCKED, result.failure());
        assertEquals("Tài khoản đã bị khóa. Liên hệ Admin để mở khóa.", result.failureMessage());
    }

    @Test
    void requiresExactlyOneOutcome() {
        assertThrows(IllegalArgumentException.class, () -> new LoginResult(null, null));
    }

    @Test
    void everyFailureReasonHasAVietnameseMessage() {
        for (LoginFailureReason reason : LoginFailureReason.values()) {
            assertFalse(reason.message().isBlank(), reason.name() + " must have a message");
        }
    }

    @Test
    void unavailableServiceRejectsEveryAttempt() {
        LoginResult result = LoginService.unavailable().login("admin", "Admin@123".toCharArray());

        assertFalse(result.isSuccess());
        assertEquals(LoginFailureReason.SYSTEM_UNAVAILABLE, result.failure());
    }
}
