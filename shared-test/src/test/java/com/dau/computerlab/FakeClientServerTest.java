package com.dau.computerlab;

import com.dau.labmanagement.test.FakeClient;
import com.dau.labmanagement.test.FakeServer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeClientServerTest {
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void rejectsInvalidTimeouts() {
        assertThrows(IllegalArgumentException.class, () -> FakeServer.start(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> FakeClient.connect(
                InetAddress.getLoopbackAddress(), 1, Duration.ZERO, IO_TIMEOUT));
    }

    @Test
    void connectsOverLoopbackAndExchangesHeartbeatCommandAndAcknowledgement() throws Exception {
        try (FakeServer server = FakeServer.start(IO_TIMEOUT)) {
            assertTrue(InetAddress.getByName("127.0.0.1").isLoopbackAddress());
            try (FakeClient client = FakeClient.connect(
                    InetAddress.getLoopbackAddress(), server.port(), IO_TIMEOUT, IO_TIMEOUT)) {
                server.acceptClient();

                client.sendHeartbeat("sequence=1");
                assertEquals("sequence=1", server.receiveHeartbeat());

                server.sendCommand("LOCK command-7");
                assertEquals("LOCK command-7", client.receiveCommand());
                client.acknowledge("command-7");
                assertEquals("command-7", server.receiveAcknowledgement());

                assertEquals(java.util.List.of("sequence=1"), client.sentHeartbeats());
                assertEquals(java.util.List.of("LOCK command-7"), client.receivedCommands());
                assertEquals(java.util.List.of("command-7"), client.sentAcknowledgements());
                assertEquals(java.util.List.of("command-7"), server.receivedAcknowledgements());
                assertEquals(java.util.List.of("LOCK command-7"), server.sentCommands());
                assertEquals(java.util.List.of("sequence=1"), server.receivedHeartbeats());
            }
        }
    }

    @Test
    void readAndAcceptOperationsTimeOutInsteadOfWaitingIndefinitely() throws Exception {
        Duration shortTimeout = Duration.ofMillis(150);
        try (FakeServer server = FakeServer.start(shortTimeout)) {
            assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertThrows(SocketTimeoutException.class, server::acceptClient));
        }

        try (FakeServer server = FakeServer.start(shortTimeout);
             FakeClient client = FakeClient.connect(
                     InetAddress.getLoopbackAddress(), server.port(), IO_TIMEOUT, shortTimeout)) {
            server.acceptClient();
            assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertThrows(SocketTimeoutException.class, server::receiveHeartbeat));
        }
    }

    @Test
    void clientDisconnectIsObservedAsEndOfStream() throws Exception {
        try (FakeServer server = FakeServer.start(IO_TIMEOUT)) {
            FakeClient client = FakeClient.connect(
                    InetAddress.getLoopbackAddress(), server.port(), IO_TIMEOUT, IO_TIMEOUT);
            server.acceptClient();
            client.close();

            assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertNull(server.receiveHeartbeat()));
            assertTrue(client.isClosed());
        }
    }

    @Test
    void serverDisconnectIsObservedAsEndOfStream() throws Exception {
        try (FakeServer server = FakeServer.start(IO_TIMEOUT);
             FakeClient client = FakeClient.connect(
                     InetAddress.getLoopbackAddress(), server.port(), IO_TIMEOUT, IO_TIMEOUT)) {
            server.acceptClient();
            server.disconnectClient();

            assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> assertNull(client.receiveCommand()));
            assertFalse(client.isClosed());
        }
    }

    @Test
    void closingServerReleasesItsSocketResources() throws Exception {
        try (FakeServer server = FakeServer.start(IO_TIMEOUT);
             FakeClient client = FakeClient.connect(
                     InetAddress.getLoopbackAddress(), server.port(), IO_TIMEOUT, IO_TIMEOUT)) {
            server.acceptClient();

            server.close();

            assertTrue(server.isClosed());
            assertFalse(client.isClosed());
            client.close();
            assertTrue(client.isClosed());
        }
    }
}