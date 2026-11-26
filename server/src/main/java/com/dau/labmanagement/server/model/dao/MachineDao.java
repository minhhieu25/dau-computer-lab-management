package com.dau.labmanagement.server.model.dao;

import com.dau.labmanagement.server.model.entity.MachineRow;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for the {@code machine} table. Heartbeat updates keep
 * last_seen_at / last_rtt_ms / packet_loss_percent fresh for the status grid (BR-01).
 */
public interface MachineDao {

    @SqlQuery("""
            INSERT INTO machine (machine_guid, hostname, ip_address, seat_label)
            VALUES (:machineGuid, :hostname, :ipAddress, :seatLabel)
            RETURNING machine_id""")
    long insert(@Bind("machineGuid") UUID machineGuid,
                @Bind("hostname") String hostname,
                @Bind("ipAddress") String ipAddress,
                @Bind("seatLabel") String seatLabel);

    @SqlQuery("""
            INSERT INTO machine (machine_guid, hostname, ip_address, seat_label)
            VALUES (:machineGuid, :hostname, :ipAddress, :seatLabel)
            ON CONFLICT (machine_guid) DO UPDATE
            SET hostname = EXCLUDED.hostname, ip_address = EXCLUDED.ip_address
            RETURNING machine_id""")
    long upsert(@Bind("machineGuid") UUID machineGuid,
                @Bind("hostname") String hostname,
                @Bind("ipAddress") String ipAddress,
                @Bind("seatLabel") String seatLabel);

    @SqlQuery("""
            SELECT machine_id, machine_guid, hostname, ip_address, seat_label,
                   last_seen_at, last_rtt_ms, packet_loss_percent
            FROM machine
            WHERE machine_id = :machineId""")
    Optional<MachineRow> findById(@Bind("machineId") long machineId);

    @SqlQuery("""
            SELECT machine_id, machine_guid, hostname, ip_address, seat_label,
                   last_seen_at, last_rtt_ms, packet_loss_percent
            FROM machine
            WHERE machine_guid = :machineGuid""")
    Optional<MachineRow> findByGuid(@Bind("machineGuid") UUID machineGuid);

    @SqlQuery("""
            SELECT machine_id, machine_guid, hostname, ip_address, seat_label,
                   last_seen_at, last_rtt_ms, packet_loss_percent
            FROM machine
            ORDER BY machine_id""")
    List<MachineRow> findAll();

    @SqlUpdate("""
            UPDATE machine
            SET last_seen_at = :lastSeenAt, last_rtt_ms = :lastRttMs, packet_loss_percent = :packetLossPercent
            WHERE machine_guid = :machineGuid""")
    int updateHeartbeat(@Bind("machineGuid") UUID machineGuid,
                        @Bind("lastSeenAt") OffsetDateTime lastSeenAt,
                        @Bind("lastRttMs") Integer lastRttMs,
                        @Bind("packetLossPercent") BigDecimal packetLossPercent);

    @SqlUpdate("""
            UPDATE machine
            SET last_seen_at = :lastSeenAt
            WHERE machine_guid = :machineGuid""")
    int updateLastSeen(@Bind("machineGuid") UUID machineGuid,
                       @Bind("lastSeenAt") OffsetDateTime lastSeenAt);

    @SqlQuery("""
            SELECT EXISTS (
                SELECT 1
                FROM machine
                WHERE machine_guid = :machineGuid
                  AND last_seen_at > :offlineSince
            )""")
    boolean isOnline(@Bind("machineGuid") UUID machineGuid,
                     @Bind("offlineSince") OffsetDateTime offlineSince);

    @SqlUpdate("UPDATE machine SET seat_label = :seatLabel WHERE machine_id = :machineId")
    int updateSeatLabel(@Bind("machineId") long machineId, @Bind("seatLabel") String seatLabel);
}
