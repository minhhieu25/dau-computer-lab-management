package com.dau.labmanagement.server.network.tcp;

import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.server.model.dao.MachineDao;
import com.dau.labmanagement.server.network.udp.HeartbeatReceiver;
import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.AckMessage;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import com.dau.labmanagement.shared.security.PasswordProof;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSessionHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PASSWORD_ITERATIONS = 210_000;

    @Test
    void authenticatesOnlyActiveStudentsAndKeepsFailedLoginsOnline() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
            Jdbi jdbi = JdbiProvider.create(dataSource);
            long studentUserId = insertUser(dataSource, "2101207", "Minh Khoa", "student", "active");
            insertUser(dataSource, "2101208", "Locked Student", "student", "locked");
            insertUser(dataSource, "teacher", "Teacher", "teacher", "active");
            String studentPasswordHash = createPasswordHash("CorrectPassword!");
            replacePasswordHash(dataSource, "2101207", studentPasswordHash);
            replacePasswordHash(dataSource, "2101208", studentPasswordHash);
            replacePasswordHash(dataSource, "teacher", studentPasswordHash);

            UUID clientId = UUID.randomUUID();
            try (HeartbeatReceiver heartbeats = new HeartbeatReceiver(
                        jdbi, new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
                 TcpServer server = new TcpServer(
                         0, new ClientSessionHandler(jdbi, "test-server"))) {
                heartbeats.start();
                server.start();
                long studentSessionId;
                try (Socket client = connect(clientId, server.localPort())) {
                    sendHeartbeat(clientId, heartbeats.localPort());
                    OffsetDateTime firstHeartbeat = readLastSeen(dataSource, clientId);
                    assertNotNull(firstHeartbeat,
                            "A connected but unidentified machine is Online by heartbeat");
                    assertTrue(isOnline(jdbi, clientId, firstHeartbeat.plusSeconds(1)),
                            "A recent registered heartbeat represents an Online machine");
                    assertEquals(0, countRows(dataSource, "student_session"));

                    LoginAckMessage rejectedPassword = login(client, "2101207", "WrongPassword!");
                    assertEquals(LoginStatus.REJECTED, rejectedPassword.status());
                    assertEquals(0, countRows(dataSource, "student_session"));
                    Thread.sleep(2);
                    sendHeartbeat(clientId, heartbeats.localPort());
                    OffsetDateTime lastSeenAfterFailure = readLastSeen(dataSource, clientId);
                    assertTrue(lastSeenAfterFailure.isAfter(firstHeartbeat),
                            "Failed authentication must leave heartbeat state updating");
                    assertTrue(isOnline(jdbi, clientId, lastSeenAfterFailure.plusSeconds(1)),
                            "The machine remains Online after authentication is rejected");
                    assertTrue(!isOnline(jdbi, clientId, lastSeenAfterFailure.plusSeconds(31)),
                            "The machine becomes Offline after the heartbeat timeout");

                    LoginAckMessage unknownStudent = login(client, "9999999", "CorrectPassword!");
                    assertEquals(LoginStatus.REJECTED, unknownStudent.status());
                    assertEquals(rejectedPassword.message(), unknownStudent.message());
                    assertEquals(0, countRows(dataSource, "student_session"));

                    assertEquals(LoginStatus.REJECTED,
                            login(client, "2101208", "CorrectPassword!").status());
                    assertEquals(LoginStatus.REJECTED,
                            login(client, "teacher", "CorrectPassword!").status());
                    assertEquals(0, countRows(dataSource, "student_session"));

                    assertEquals(1, countRows(dataSource, "machine"),
                            "HELLO registers a connected machine before any login");
                    assertEquals(0, countRows(dataSource, "student_session"),
                            "An online machine has no student identity before authentication");

                    LoginAckMessage authenticated = login(client, "2101207", "CorrectPassword!");
                    assertEquals(LoginStatus.AUTHENTICATED, authenticated.status());
                    assertEquals("2101207", authenticated.studentId());
                    assertEquals("Minh Khoa", authenticated.fullName());
                    assertNotNull(authenticated.studentSessionId());
                    studentSessionId = authenticated.studentSessionId();

                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 SELECT ss.user_id, ss.machine_id, ss.logout_at,
                                        m.machine_guid
                                 FROM student_session ss
                                 JOIN machine m ON m.machine_id = ss.machine_id
                                 WHERE ss.student_session_id = ?
                                 """)) {
                        statement.setLong(1, authenticated.studentSessionId());
                        try (ResultSet result = statement.executeQuery()) {
                            assertTrue(result.next());
                            assertEquals(studentUserId, result.getLong("user_id"));
                            assertEquals(clientId, UUID.fromString(result.getString("machine_guid")));
                            assertNull(result.getObject("logout_at"));
                        }
                    }
                }
                assertTrue(awaitSessionClosed(dataSource, studentSessionId),
                        "A TCP disconnect should close its active student session");
            }
        }
    }

    private static Socket connect(UUID clientId, int port) throws Exception {
        Socket client = new Socket();
        client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2_000);
        client.setSoTimeout(5_000);
        TcpFrameCodec.write(client.getOutputStream(), frame(
                MessageType.HELLO,
                new HelloMessage(clientId.toString(), "test-client")));
        ProtocolFrame helloAck = TcpFrameCodec.read(client.getInputStream());
        assertEquals(MessageType.HELLO_ACK, helloAck.type());
        assertEquals("test-server",
                TcpFrameCodec.payloadAs(helloAck, HelloAckMessage.class).serverId());
        return client;
    }

    private static LoginAckMessage login(Socket client, String studentId, String password)
            throws Exception {
        TcpFrameCodec.write(client.getOutputStream(), frame(
                MessageType.LOGIN, new LoginMessage(studentId, null, null)));
        LoginAckMessage challenge = readLoginAck(client);
        assertEquals(LoginStatus.CHALLENGE, challenge.status());

        char[] passwordCharacters = password.toCharArray();
        byte[] key = null;
        try {
            key = PasswordProof.deriveKey(
                    passwordCharacters, challenge.salt(), challenge.iterations());
            String proof = PasswordProof.createProof(key, challenge.nonce());
            assertNotEquals(password, proof, "The plaintext password must not be sent as proof");
            TcpFrameCodec.write(client.getOutputStream(), frame(
                    MessageType.LOGIN,
                    new LoginMessage(studentId, challenge.nonce(), proof)));
            return readLoginAck(client);
        } finally {
            java.util.Arrays.fill(passwordCharacters, '\0');
            if (key != null) {
                java.util.Arrays.fill(key, (byte) 0);
            }
        }
    }

    private static LoginAckMessage readLoginAck(Socket client) throws Exception {
        ProtocolFrame response = TcpFrameCodec.read(client.getInputStream());
        assertNotNull(response);
        assertEquals(MessageType.LOGIN_ACK, response.type());
        return TcpFrameCodec.payloadAs(response, LoginAckMessage.class);
    }

    private static ProtocolFrame frame(MessageType type, Object payload) {
        return new ProtocolFrame(
                type, ProtocolVersion.CURRENT, OBJECT_MAPPER.valueToTree(payload));
    }

    private static long insertUser(
            DataSource dataSource, String username, String fullName, String role, String status)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO app_user (username, password_hash, full_name, role, status)
                     VALUES (?, 'pending', ?, ?, ?)
                     RETURNING user_id
                     """)) {
            statement.setString(1, username);
            statement.setString(2, fullName);
            statement.setString(3, role);
            statement.setString(4, status);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private static void replacePasswordHash(DataSource dataSource, String username, String hash)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE app_user SET password_hash = ? WHERE username = ?")) {
            statement.setString(1, hash);
            statement.setString(2, username);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static String createPasswordHash(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        char[] passwordCharacters = password.toCharArray();
        byte[] key = PasswordProof.deriveKey(passwordCharacters, encodedSalt, PASSWORD_ITERATIONS);
        try {
            return "pbkdf2-sha256$" + PASSWORD_ITERATIONS + "$" + encodedSalt + "$"
                    + Base64.getEncoder().encodeToString(key);
        } finally {
            java.util.Arrays.fill(passwordCharacters, '\0');
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    private static int countRows(DataSource dataSource, String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM " + table);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static boolean awaitSessionClosed(DataSource dataSource, long sessionId)
            throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT logout_at FROM student_session WHERE student_session_id = ?")) {
                statement.setLong(1, sessionId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getObject("logout_at") != null) {
                        return true;
                    }
                }
            }
            Thread.sleep(10);
        }
        return false;
    }

    private static void sendHeartbeat(UUID clientId, int heartbeatPort) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2_000);
            byte[] data = UdpFrameCodec.encode(frame(
                    MessageType.HEARTBEAT,
                    new HeartbeatMessage(clientId.toString(), 1, System.currentTimeMillis())));
            socket.send(new DatagramPacket(
                    data, data.length, InetAddress.getLoopbackAddress(), heartbeatPort));

            byte[] responseData = new byte[1201];
            DatagramPacket response = new DatagramPacket(responseData, responseData.length);
            socket.receive(response);
            ProtocolFrame acknowledgment = UdpFrameCodec.decode(
                    response.getData(), response.getLength());
            assertEquals(MessageType.ACK, acknowledgment.type());
            assertEquals(clientId.toString(),
                    UdpFrameCodec.payloadAs(acknowledgment, AckMessage.class).clientId());
        }
    }

    private static OffsetDateTime readLastSeen(DataSource dataSource, UUID clientId)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT last_seen_at FROM machine WHERE machine_guid = ?")) {
            statement.setObject(1, clientId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getObject("last_seen_at", OffsetDateTime.class);
            }
        }
    }

    private static boolean isOnline(Jdbi jdbi, UUID clientId, OffsetDateTime now) {
        return jdbi.withExtension(MachineDao.class,
                dao -> dao.isOnline(clientId, now.minusSeconds(30)));
    }

}
