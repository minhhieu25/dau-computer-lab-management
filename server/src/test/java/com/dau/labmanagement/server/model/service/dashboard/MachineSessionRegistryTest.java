package com.dau.labmanagement.server.model.service.dashboard;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class MachineSessionRegistryTest {

    private MachineSessionRegistry createRegistry() {

        return new MachineSessionRegistry(
                Clock.fixed(
                        Instant.parse("2026-01-01T00:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void registerMachine() {

        var registry = createRegistry();

        registry.register(
                "pc01", "PC-01", "127.0.0.1", "A01");

        assertEquals(1, registry.size());

        var machine = registry.snapshots().getFirst();

        assertEquals("PC-01", machine.hostname());
        assertEquals("A01", machine.seatLabel());
        assertEquals("Chưa đăng nhập", machine.displayStudent());
    }

    @Test
    void heartbeatUpdatesMetrics() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        assertTrue(registry.heartbeat(
                "pc01", 1, 25.0, 0.0));

        var machine = registry.snapshots().getFirst();

        assertEquals(25.0, machine.rttMillis());
        assertEquals(0.0, machine.packetLossPercent());
        assertEquals(MachineStatus.ONLINE, machine.status());
    }

    @Test
    void duplicateHeartbeatIsIgnored() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        assertTrue(registry.heartbeat(
                "pc01", 1, 20.0, 0.0));

        assertFalse(registry.heartbeat(
                "pc01", 1, 500.0, 50.0));

        assertEquals(
                20.0,
                registry.snapshots().getFirst().rttMillis());
    }

    @Test
    void unknownMachineIsRejected() {

        var registry = createRegistry();

        assertFalse(registry.heartbeat(
                "unknown", 1, 10.0, 0.0));
    }

    @Test
    void unstableNetworkIsYellow() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        registry.heartbeat("pc01", 1, 101.0, 5.1);

        assertEquals(
                MachineStatus.UNSTABLE,
                registry.snapshots().getFirst().status());
    }

    @Test
    void studentLoginAndLogout() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        assertTrue(registry.login(
                "pc01", "SV001", "Sinh vien A"));

        assertEquals(
                "Sinh vien A",
                registry.snapshots().getFirst().displayStudent());

        registry.logout("pc01");

        assertEquals(
                "Chưa đăng nhập",
                registry.snapshots().getFirst().displayStudent());
    }

    @Test
    void disconnectedMachineIsRed() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        registry.disconnect("pc01");

        assertEquals(
                MachineStatus.OFFLINE,
                registry.snapshots().getFirst().status());
    }

    @Test
    void reconnectResetsSequence() {

        var registry = createRegistry();

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");
        registry.heartbeat("pc01", 100, 20.0, 0.0);

        registry.disconnect("pc01");

        registry.register("pc01", "PC-01", "127.0.0.1", "A01");

        assertTrue(registry.heartbeat(
                "pc01", 1, 10.0, 0.0));

        assertEquals(1, registry.size());
    }

    @Test
    void disconnectedMachineRejectsLateHeartbeat() {

        var registry = createRegistry();

        registry.register(
                "pc01", "PC-01", "127.0.0.1", "A01");

        registry.heartbeat("pc01", 1, 20.0, 0.0);
        registry.disconnect("pc01");

        assertFalse(registry.heartbeat(
                "pc01", 2, 10.0, 0.0));

        assertEquals(
                MachineStatus.OFFLINE,
                registry.snapshots().getFirst().status());
    }

    @Test
    void disconnectedMachineRejectsDatabaseStudent() {

        var registry = createRegistry();

        registry.register(
                "pc01", "PC-01", "127.0.0.1", "A01");

        var registeredAt = registry.registeredAt("pc01");

        registry.disconnect("pc01");

        registry.updateStudentFromDatabase(
                "pc01", registeredAt, "SV001", "Student A");

        assertNull(
                registry.snapshots().getFirst().studentId());
    }

    @Test
    void databaseStudentUpdatesConnectedMachine() {

        var registry = createRegistry();

        registry.register(
                "pc01", "PC-01", "127.0.0.1", "A01");

        registry.updateStudentFromDatabase(
                "pc01",
                registry.registeredAt("pc01"),
                "SV001",
                "Student A");

        assertEquals(
                "SV001",
                registry.snapshots().getFirst().studentId());
    }

    @Test
    void oldRegistrationCannotUpdateStudent() {

        var registry = createRegistry();

        registry.register(
                "pc01", "PC-01", "127.0.0.1", "A01");

        registry.updateStudentFromDatabase(
                "pc01",
                registry.registeredAt("pc01").minusSeconds(60),
                "SV001",
                "Student A");

        assertNull(
                registry.snapshots().getFirst().studentId());
    }
}
