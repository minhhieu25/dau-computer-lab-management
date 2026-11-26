package com.dau.labmanagement.server.network.udp;

import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.server.model.dao.MachineDao;
import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.AckMessage;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatReceiverTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void persistsOnlineHeartbeatBeforeAcknowledgingRegisteredMachine() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
            Jdbi jdbi = JdbiProvider.create(dataSource);
            UUID clientId = UUID.randomUUID();
            long machineId = jdbi.withExtension(MachineDao.class, dao ->
                    dao.upsert(clientId, "test-client", "127.0.0.1", null));

            try (HeartbeatReceiver receiver = new HeartbeatReceiver(
                    jdbi, new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
             DatagramSocket client = new DatagramSocket()) {
                receiver.start();
                client.setSoTimeout((int) Duration.ofSeconds(2).toMillis());

                byte[] request = UdpFrameCodec.encode(new ProtocolFrame(
                        MessageType.HEARTBEAT,
                        ProtocolVersion.CURRENT,
                        OBJECT_MAPPER.valueToTree(
                                new HeartbeatMessage(clientId.toString(), 7, 1000))));
                client.send(new DatagramPacket(
                        request, request.length,
                        InetAddress.getLoopbackAddress(), receiver.localPort()));

                byte[] responseBuffer = new byte[1201];
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                client.receive(response);
                ProtocolFrame frame = UdpFrameCodec.decode(
                        response.getData(), response.getLength());

                assertEquals(MessageType.ACK, frame.type());
                AckMessage acknowledgment = UdpFrameCodec.payloadAs(frame, AckMessage.class);
                assertEquals(clientId.toString(), acknowledgment.clientId());
                assertEquals(7, acknowledgment.sequence());
                assertTrue(acknowledgment.serverReceivedAtEpochMillis() > 0);

                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT last_seen_at FROM machine WHERE machine_id = ?")) {
                    statement.setLong(1, machineId);
                    try (ResultSet result = statement.executeQuery()) {
                        assertTrue(result.next());
                        assertNotNull(result.getObject("last_seen_at"),
                                "Online state is based on persisted heartbeat time");
                    }
                }
            }
        }
    }
}
