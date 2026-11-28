package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.BeaconMessage;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import com.dau.labmanagement.shared.security.PasswordProof;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientNetworkRuntimeLoginTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String STUDENT_ID = "2101207";
    private static final String PASSWORD = "CorrectPassword!";
    private static final String NONCE =
            Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    private static final String SALT = createSalt();
    private static final int ITERATIONS = 100;

    @Test
    void performsChallengeLoginAndKeepsConnectionAfterRejection() throws Exception {
        int beaconPort;
        try (DatagramSocket probe = new DatagramSocket(0)) {
            beaconPort = probe.getLocalPort();
        }

        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        byte[] expectedKey = PasswordProof.deriveKey(
                PASSWORD.toCharArray(), SALT, ITERATIONS);
        try (ServerSocket server = new ServerSocket(0);
             ClientNetworkRuntime runtime = new ClientNetworkRuntime(
                     status -> {
                         if (status.startsWith("Connected to server")) {
                             connected.countDown();
                         }
                     },
                     "test-client",
                     beaconPort);
             DatagramSocket beaconSender = new DatagramSocket()) {
            Thread serverThread = Thread.ofVirtual().start(() ->
                    acceptAndHandle(server, expectedKey, serverFailure));
            runtime.start();
            sendBeacon(beaconSender, beaconPort, server.getLocalPort());
            assertTrue(connected.await(5, TimeUnit.SECONDS),
                    "Client should connect after receiving a valid beacon");

            char[] incorrectPassword = "WrongPassword!".toCharArray();
            LoginAckMessage rejected = runtime.loginStudent("invalid-student", incorrectPassword);
            assertEquals(LoginStatus.REJECTED, rejected.status());
            assertTrue(allCleared(incorrectPassword), "Login must clear the supplied password");

            char[] correctPassword = PASSWORD.toCharArray();
            LoginAckMessage authenticated = runtime.loginStudent(STUDENT_ID, correctPassword);
            assertEquals(LoginStatus.AUTHENTICATED, authenticated.status());
            assertEquals(STUDENT_ID, authenticated.studentId());
            assertEquals("Minh Khoa", authenticated.fullName());
            assertTrue(allCleared(correctPassword), "Login must clear the supplied password");
            assertNull(serverFailure.get());
            runtime.close();
            server.close();
            serverThread.join(5_000);
            assertFalse(serverThread.isAlive(), "Fake server should stop after Client disconnects");
        } finally {
            Arrays.fill(expectedKey, (byte) 0);
        }
    }

    private static void acceptAndHandle(
            ServerSocket server, byte[] expectedKey, AtomicReference<Throwable> serverFailure) {
        try (Socket socket = server.accept()) {
            handleClient(socket, expectedKey, serverFailure);
        } catch (Throwable failure) {
            serverFailure.set(failure);
        }
    }

    private static void handleClient(
            Socket socket, byte[] expectedKey, AtomicReference<Throwable> serverFailure) {
        try (socket) {
            socket.setSoTimeout(5_000);
            ProtocolFrame hello = TcpFrameCodec.read(socket.getInputStream());
            assertEquals(MessageType.HELLO, hello.type());
            write(socket, MessageType.HELLO_ACK, new HelloAckMessage("test-server", 1));

            ProtocolFrame frame;
            while ((frame = TcpFrameCodec.read(socket.getInputStream())) != null) {
                assertEquals(MessageType.LOGIN, frame.type());
                LoginMessage firstRequest = TcpFrameCodec.payloadAs(frame, LoginMessage.class);
                assertNull(firstRequest.nonce());
                assertNull(firstRequest.proof());
                assertFalse(frame.payload().has("password"),
                        "LOGIN must never include a plaintext password property");

                write(socket, MessageType.LOGIN_ACK, new LoginAckMessage(
                        LoginStatus.CHALLENGE, NONCE, SALT, ITERATIONS,
                        null, null, null, null));
                ProtocolFrame proofFrame = TcpFrameCodec.read(socket.getInputStream());
                LoginMessage proofRequest = TcpFrameCodec.payloadAs(proofFrame, LoginMessage.class);
                assertEquals(firstRequest.studentId(), proofRequest.studentId());
                assertFalse(PASSWORD.equals(proofRequest.proof()),
                        "The transmitted proof must not be the plaintext password");
                boolean valid = STUDENT_ID.equals(proofRequest.studentId())
                        && PasswordProof.createProof(expectedKey, NONCE).equals(proofRequest.proof());
                LoginAckMessage result = valid
                        ? new LoginAckMessage(LoginStatus.AUTHENTICATED, null, null, 0,
                                11L, STUDENT_ID, "Minh Khoa", null)
                        : new LoginAckMessage(LoginStatus.REJECTED, null, null, 0,
                                null, null, null, "Invalid credentials");
                write(socket, MessageType.LOGIN_ACK, result);
            }
        } catch (Throwable failure) {
            serverFailure.set(failure);
        }
    }

    private static void sendBeacon(DatagramSocket sender, int port, int tcpPort) throws Exception {
        BeaconMessage beacon = new BeaconMessage(
                "test-server", tcpPort, ProtocolVersion.CURRENT, System.currentTimeMillis());
        byte[] data = com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec.encode(
                new ProtocolFrame(
                        MessageType.BEACON,
                        ProtocolVersion.CURRENT,
                        OBJECT_MAPPER.valueToTree(beacon)));
        sender.send(new DatagramPacket(
                data, data.length, InetAddress.getLoopbackAddress(), port));
    }

    private static void write(Socket socket, MessageType type, Object payload) throws Exception {
        TcpFrameCodec.write(socket.getOutputStream(), new ProtocolFrame(
                type, ProtocolVersion.CURRENT, OBJECT_MAPPER.valueToTree(payload)));
    }

    private static String createSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static boolean allCleared(char[] value) {
        for (char character : value) {
            if (character != '\0') {
                return false;
            }
        }
        return true;
    }
}
