package com.dau.labmanagement.server;

import com.dau.labmanagement.server.model.state.TransferProgress;
import com.dau.labmanagement.server.model.state.TransferStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferProgressTest {
    @Test
    void tracksSentAndAcknowledgedBytesSeparatelyAndCalculatesMetrics() {
        AtomicLong clock = new AtomicLong();
        TransferProgress progress = progress("pc-1", "student-1", 250_000, clock);
        progress.start();
        progress.recordBytesSent(125_000);
        clock.set(Duration.ofSeconds(1).toNanos());
        progress.acknowledgeBytesWritten(125_000);

        assertEquals(125_000, progress.bytesSent());
        assertEquals(125_000, progress.bytesAcknowledged());
        assertEquals(1.0, progress.speedMbps(), 0.001);
        assertEquals(50.0, progress.percentComplete(), 0.001);
        assertEquals(Optional.of(Duration.ofSeconds(1)), progress.eta());
        assertEquals(TransferStatus.TRANSFERRING, progress.status());
    }

    @Test
    void emptyFileCompletesWithoutDivisionByZero() {
        TransferProgress progress = progress("pc-1", "student-1", 0, new AtomicLong());

        progress.complete();

        assertEquals(100.0, progress.percentComplete());
        assertEquals(Optional.of(Duration.ZERO), progress.eta());
        assertEquals(0.0, progress.speedMbps());
        assertEquals(TransferStatus.COMPLETED, progress.status());
    }

    @Test
    void preventsAcknowledgingUnsentBytesAndCompletingIncompleteTransfer() {
        TransferProgress progress = progress("pc-1", "student-1", 100, new AtomicLong());
        progress.start();

        assertThrows(IllegalArgumentException.class, () -> progress.acknowledgeBytesWritten(1));
        assertThrows(IllegalStateException.class, progress::complete);
        assertThrows(IllegalArgumentException.class, () -> progress.recordBytesSent(101));
    }

    @Test
    void capsPercentageAtOneHundredAndRejectsUpdatesAfterCompletion() {
        TransferProgress progress = progress("pc-1", "student-1", 10, new AtomicLong());
        progress.recordBytesSent(10);
        progress.acknowledgeBytesWritten(10);
        progress.complete();

        assertEquals(100.0, progress.percentComplete());
        assertThrows(IllegalStateException.class, () -> progress.recordBytesSent(1));
    }

    @Test
    void etaIsZeroAfterAllBytesAreAcknowledgedBeforeFinalCompletion() {
        TransferProgress progress = progress("pc-1", "student-1", 10, new AtomicLong());
        progress.start();
        progress.recordBytesSent(10);
        progress.acknowledgeBytesWritten(10);

        assertEquals(Optional.of(Duration.ZERO), progress.eta());
        assertEquals(TransferStatus.TRANSFERRING, progress.status());
    }

    @Test
    void keepsClientProgressIndependentAndThreadSafe() throws InterruptedException {
        TransferProgress first = progress("pc-1", "student-1", 1_000, new AtomicLong());
        TransferProgress second = progress("pc-2", "student-2", 1_000, new AtomicLong());
        first.start();
        second.start();
        first.recordBytesSent(1_000);
        second.recordBytesSent(1_000);

        Thread firstAcks = new Thread(() -> acknowledgeRepeatedly(first, 100));
        Thread secondAcks = new Thread(() -> acknowledgeRepeatedly(second, 200));
        firstAcks.start();
        secondAcks.start();
        firstAcks.join();
        secondAcks.join();

        assertEquals(100L, first.bytesAcknowledged());
        assertEquals(200L, second.bytesAcknowledged());
        assertEquals(10.0, first.percentComplete(), 0.001);
        assertEquals(20.0, second.percentComplete(), 0.001);
    }

    @Test
    void etaIsUnavailableBeforeAcknowledgedRateExists() {
        TransferProgress progress = progress("pc-1", "student-1", 100, new AtomicLong());

        assertFalse(progress.eta().isPresent());
        assertTrue(progress.percentComplete() == 0.0);
    }

    private static TransferProgress progress(
            String machineId, String studentId, long totalBytes, AtomicLong clock) {
        return new TransferProgress(UUID.randomUUID(), machineId, Optional.of(studentId),
                "assignment.bin", totalBytes, clock::get);
    }

    private static void acknowledgeRepeatedly(TransferProgress progress, int count) {
        for (int index = 0; index < count; index++) {
            progress.acknowledgeBytesWritten(1);
        }
    }
}