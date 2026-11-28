package com.dau.labmanagement.server.model.service.dashboard;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MachineSessionRegistry {

    private final ConcurrentMap<String, Session> sessions =
            new ConcurrentHashMap<>();

    private final Clock clock;

    public MachineSessionRegistry() {
        this(Clock.systemUTC());
    }

    public MachineSessionRegistry(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public void register(
            String clientId,
            String hostname,
            String ipAddress,
            String seatLabel) {

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Invalid clientId");
        }

        sessions.put(clientId, new Session(
                clientId, hostname, ipAddress,
                seatLabel, clock.instant()));
    }

    public boolean heartbeat(
            String clientId,
            long sequence,
            Double rttMillis,
            Double packetLossPercent) {

        Session session = sessions.get(clientId);

        if (session == null || sequence < 1) {
            return false;
        }

        MachineStatusEvaluator.evaluate(
                0, rttMillis, packetLossPercent);

        synchronized (session) {

            if (!session.connected || sequence <= session.lastSequence) {
                return false;
            }

            session.lastSequence = sequence;
            session.lastHeartbeat = clock.instant();
            session.rttMillis = rttMillis;
            session.packetLossPercent = packetLossPercent;


            return true;
        }
    }

    public boolean login(
            String clientId,
            String studentId,
            String studentName) {

        Session session = sessions.get(clientId);

        if (session == null) {
            return false;
        }

        synchronized (session) {
            session.studentId = studentId;
            session.studentName = studentName;
            return true;
        }
    }

    public void logout(String clientId) {

        Session session = sessions.get(clientId);

        if (session != null) {
            synchronized (session) {
                session.studentId = null;
                session.studentName = null;
            }
        }
    }

    public void disconnect(String clientId) {

        Session session = sessions.get(clientId);

        if (session != null) {
            synchronized (session) {
                session.connected = false;
                session.studentId = null;
                session.studentName = null;
            }
        }
    }

    public List<MachineSnapshot> snapshots() {

        Instant now = clock.instant();

        return sessions.values().stream()
                .map(session -> snapshot(session, now))
                .sorted(Comparator.comparing(
                        MachineSnapshot::clientId))
                .toList();
    }

    public void updateSeatLabel(
            String clientId,
            String seatLabel) {

        Session session = sessions.get(clientId);

        if (session != null) {
            synchronized (session) {
                session.seatLabel = seatLabel;
            }
        }
    }

    public Instant registeredAt(String clientId) {

        Session session = sessions.get(clientId);

        return session == null
                ? null
                : session.registeredAt;
    }

    public void updateStudentFromDatabase(
            String clientId,
            Instant expectedRegistration,
            String studentId,
            String studentName) {

        sessions.computeIfPresent(clientId, (id, session) -> {
            synchronized (session) {

                if (!session.registeredAt.equals(expectedRegistration)) {
                    return session;
                }

                long elapsed = Math.max(
                        0,
                        clock.millis()
                                - session.lastHeartbeat.toEpochMilli());

                boolean offline = !session.connected
                        || MachineStatusEvaluator.evaluate(
                                elapsed,
                                session.rttMillis,
                                session.packetLossPercent)
                                == MachineStatus.OFFLINE;

                session.studentId = offline ? null : studentId;
                session.studentName = offline ? null : studentName;

                return session;
            }
        });
    }

    public int size() {
        return sessions.size();
    }

    private MachineSnapshot snapshot(
            Session session, Instant now) {

        synchronized (session) {

            long elapsed = Math.max(
                    0,
                    now.toEpochMilli()
                            - session.lastHeartbeat.toEpochMilli());

            MachineStatus status = session.connected
                    ? MachineStatusEvaluator.evaluate(
                            elapsed,
                            session.rttMillis,
                            session.packetLossPercent)
                    : MachineStatus.OFFLINE;

            return new MachineSnapshot(
                    session.clientId,
                    session.hostname,
                    session.ipAddress,
                    session.seatLabel,
                    session.studentId,
                    session.studentName,
                    session.lastHeartbeat,
                    session.rttMillis,
                    session.packetLossPercent,
                    status);
        }
    }

    private static final class Session {

        final String clientId;
        final String hostname;
        final String ipAddress;
        String seatLabel;

        String studentId;
        String studentName;

        final Instant registeredAt;
        Instant lastHeartbeat;
        long lastSequence;

        Double rttMillis;
        Double packetLossPercent;

        boolean connected = true;

        Session(
                String clientId,
                String hostname,
                String ipAddress,
                String seatLabel,
                Instant registeredAt) {

            this.clientId = clientId;
            this.hostname = hostname;
            this.ipAddress = ipAddress;
            this.seatLabel = seatLabel;
            this.registeredAt = registeredAt;
            this.lastHeartbeat = registeredAt;
        }
    }
}
