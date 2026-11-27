package com.dau.labmanagement.server.model.service.dashboard;

import com.dau.labmanagement.server.model.dao.MachineDao;
import com.dau.labmanagement.server.model.dao.StudentSessionDao;
import com.dau.labmanagement.server.model.dao.UserDao;
import com.dau.labmanagement.server.model.entity.MachineRow;
import com.dau.labmanagement.server.model.entity.UserRow;

import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import com.dau.labmanagement.server.model.entity.StudentSessionRow;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DashboardDatabaseService
        implements AutoCloseable {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    DashboardDatabaseService.class);

    private final Jdbi jdbi;
    private final MachineSessionRegistry registry;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(
                        task, "dashboard-database-sync");
                thread.setDaemon(true);
                return thread;
            });

    public DashboardDatabaseService(
            Jdbi jdbi,
            MachineSessionRegistry registry) {

        this.jdbi = Objects.requireNonNull(jdbi);

        this.jdbi.registerRowMapper(
                ConstructorMapper.factory(MachineRow.class));

        this.jdbi.registerRowMapper(
                ConstructorMapper.factory(StudentSessionRow.class));

        this.jdbi.registerRowMapper(
                ConstructorMapper.factory(UserRow.class));

        LOG.info("SC-003 dashboard row mappers registered");
        this.registry = Objects.requireNonNull(registry);
    }

    public void start() {
        executor.scheduleWithFixedDelay(
                this::synchronize,
                0,
                3,
                TimeUnit.SECONDS
        );
    }

    private void synchronize() {
        for (MachineSnapshot machine : registry.snapshots()) {
            try {
                synchronizeMachine(machine);
            } catch (RuntimeException exception) {
                LOG.warn(
                        "Cannot synchronize machine {}",
                        machine.clientId(),
                        exception
                );
            }
        }
    }

    private void synchronizeMachine(MachineSnapshot machine) {

        UUID guid = UUID.fromString(machine.clientId());

        Instant registeredAt =
                registry.registeredAt(machine.clientId());

        if (registeredAt == null) {
            return;
        }

        Integer rtt = machine.rttMillis() == null
                ? null
                : (int) Math.min(
                        Integer.MAX_VALUE,
                        Math.round(machine.rttMillis()));

        BigDecimal loss = machine.packetLossPercent() == null
                ? null
                : BigDecimal.valueOf(
                        machine.packetLossPercent());

        DashboardData data = jdbi.inTransaction(handle -> {

            MachineDao machineDao =
                    handle.attach(MachineDao.class);

            StudentSessionDao sessionDao =
                    handle.attach(StudentSessionDao.class);

            UserDao userDao =
                    handle.attach(UserDao.class);

            machineDao.upsertDashboardMachine(
                    guid,
                    machine.hostname(),
                    machine.ipAddress(),
                    machine.lastHeartbeat()
                            .atOffset(ZoneOffset.UTC),
                    rtt,
                    loss
            );

            MachineRow row = machineDao.findByGuid(guid)
                    .orElseThrow();

            UserRow student = null;

            if (machine.status() != MachineStatus.OFFLINE) {

                student = sessionDao
                        .findActiveByMachine(row.machineId())
                        .filter(session ->
                                !session.loginAt().toInstant()
                                        .isBefore(registeredAt))
                        .flatMap(session ->
                                userDao.findById(session.userId()))
                        .filter(user ->
                                "student".equalsIgnoreCase(user.role()))
                        .filter(user ->
                                "active".equalsIgnoreCase(user.status()))
                        .filter(user -> user.deletedAt() == null)
                        .orElse(null);
            }

            return new DashboardData(
                    row.seatLabel(),
                    student == null ? null : student.username(),
                    student == null ? null : student.fullName()
            );
        });

        registry.updateSeatLabel(
                machine.clientId(), data.seatLabel());

        registry.updateStudentFromDatabase(
                machine.clientId(),
                registeredAt,
                data.studentId(),
                data.studentName()
        );
    }

    private record DashboardData(
            String seatLabel,
            String studentId,
            String studentName) {
    }

    @Override
    public void close() {
        executor.shutdownNow();

        try {
            executor.awaitTermination(
                    5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
