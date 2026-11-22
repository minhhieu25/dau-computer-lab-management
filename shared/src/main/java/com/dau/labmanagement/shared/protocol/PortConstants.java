package com.dau.labmanagement.shared.protocol;

/**
 * Well-known ports fixed at Sprint 0 (Docs/ke_hoach_trien_khai.md, section 4).
 *
 * <p>The multicast group address for the projection stream is not fixed yet;
 * it will be documented in Docs/protocol-spec.md before Sprint 3.</p>
 */
public final class PortConstants {

    /** TCP port for command/data connections (ServerSocket on the server). */
    public static final int TCP_SERVER = 50010;

    /** UDP port for LAN discovery beacons, broadcast every 2 seconds. */
    public static final int UDP_BEACON = 50000;

    /** UDP port for heartbeats, unicast every 10 seconds. */
    public static final int UDP_HEARTBEAT = 50001;

    /** UDP port for the multicast projection stream. */
    public static final int UDP_MULTICAST = 50020;

    private PortConstants() {
        // Utility class, no instances.
    }
}
