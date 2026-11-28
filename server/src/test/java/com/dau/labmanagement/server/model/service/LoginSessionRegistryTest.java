package com.dau.labmanagement.server.model.service;

import com.dau.labmanagement.server.model.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginSessionRegistryTest {

    @Test
    void opensAndFindsASession() {
        LoginSessionRegistry registry = new LoginSessionRegistry();

        LoginSession session = registry.open(7L, "gv01", "Giảng viên Một", UserRole.TEACHER);

        assertEquals(1, registry.activeCount());
        assertEquals(session, registry.find(session.sessionId()).orElseThrow());
        assertEquals("Giảng viên", session.roleLabel());
    }

    @Test
    void everySessionGetsItsOwnId() {
        LoginSessionRegistry registry = new LoginSessionRegistry();

        LoginSession first = registry.open(1L, "admin", "Admin", UserRole.ADMIN);
        LoginSession second = registry.open(2L, "gv01", "Giảng viên", UserRole.TEACHER);

        assertNotEquals(first.sessionId(), second.sessionId());
        assertEquals("Admin", first.roleLabel());
        assertEquals(2, registry.activeCount());
    }

    @Test
    void closesASession() {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        LoginSession session = registry.open(1L, "admin", "Admin", UserRole.ADMIN);

        assertTrue(registry.close(session.sessionId()));
        assertFalse(registry.close(session.sessionId()));
        assertTrue(registry.find(session.sessionId()).isEmpty());
        assertEquals(0, registry.activeCount());
    }

    @Test
    void handlesUnknownAndNullIds() {
        LoginSessionRegistry registry = new LoginSessionRegistry();

        assertTrue(registry.find(null).isEmpty());
        assertTrue(registry.find(UUID.randomUUID()).isEmpty());
        assertFalse(registry.close(null));
    }

    @Test
    void closeAllClearsEverySession() {
        LoginSessionRegistry registry = new LoginSessionRegistry();
        registry.open(1L, "admin", "Admin", UserRole.ADMIN);
        registry.open(2L, "gv01", "Giảng viên", UserRole.TEACHER);

        registry.closeAll();

        assertEquals(0, registry.activeCount());
        assertTrue(registry.activeSessions().isEmpty());
    }
}
