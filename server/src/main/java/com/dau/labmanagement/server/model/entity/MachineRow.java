package com.dau.labmanagement.server.model.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Row record for the {@code machine} table.
 *
 * @param machineId          primary key
 * @param machineGuid        unique GUID sent by the client in HELLO
 * @param hostname           OS computer name
 * @param ipAddress          IPv4 or IPv6 address
 * @param seatLabel          human-readable seat, e.g. "A01"
 * @param lastSeenAt         last heartbeat time (drives the status grid, BR-01)
 * @param lastRttMs          last measured round-trip time in milliseconds
 * @param packetLossPercent  last measured packet loss (0..100)
 */
public record MachineRow(
        Long machineId,
        UUID machineGuid,
        String hostname,
        String ipAddress,
        String seatLabel,
        OffsetDateTime lastSeenAt,
        Integer lastRttMs,
        BigDecimal packetLossPercent) {
}
