package com.dau.labmanagement.server.model.service.dashboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MachineStatusEvaluatorTest {

    @Test
    void stableConnectionIsGreen() {
        assertEquals(
                MachineStatus.ONLINE,
                MachineStatusEvaluator.evaluate(0, 20.0, 0.0)
        );
    }

    @Test
    void exactThresholdIsGreen() {
        assertEquals(
                MachineStatus.ONLINE,
                MachineStatusEvaluator.evaluate(9999, 100.0, 5.0)
        );
    }

    @Test
    void highRttIsYellow() {
        assertEquals(
                MachineStatus.UNSTABLE,
                MachineStatusEvaluator.evaluate(0, 101.0, 0.0)
        );
    }

    @Test
    void highPacketLossIsYellow() {
        assertEquals(
                MachineStatus.UNSTABLE,
                MachineStatusEvaluator.evaluate(0, 20.0, 5.1)
        );
    }

    @Test
    void oneMissedHeartbeatIsYellow() {
        assertEquals(
                MachineStatus.UNSTABLE,
                MachineStatusEvaluator.evaluate(10000, 20.0, 0.0)
        );
    }

    @Test
    void twoMissedHeartbeatsAreYellow() {
        assertEquals(
                MachineStatus.UNSTABLE,
                MachineStatusEvaluator.evaluate(20000, 20.0, 0.0)
        );
    }

    @Test
    void threeMissedHeartbeatsAreRed() {
        assertEquals(
                MachineStatus.OFFLINE,
                MachineStatusEvaluator.evaluate(30000, 20.0, 0.0)
        );
    }

    @Test
    void offlineTakesPriority() {
        assertEquals(
                MachineStatus.OFFLINE,
                MachineStatusEvaluator.evaluate(30000, 500.0, 50.0)
        );
    }

    @Test
    void missingMetricsDoNotCauseException() {
        assertEquals(
                MachineStatus.ONLINE,
                MachineStatusEvaluator.evaluate(0, null, null)
        );
    }

    @Test
    void invalidMetricsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MachineStatusEvaluator.evaluate(0, -1.0, 0.0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MachineStatusEvaluator.evaluate(0, 10.0, 101.0)
        );
    }
}