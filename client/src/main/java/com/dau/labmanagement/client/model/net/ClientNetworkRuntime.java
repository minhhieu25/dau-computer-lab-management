package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import com.dau.labmanagement.shared.security.PasswordProof;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ClientNetworkRuntime implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientNetworkRuntime.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String clientId = UUID.randomUUID().toString();
    private final String hostname;
    private final int beaconPort;
    private final Consumer<String> statusConsumer;
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private final Object lifecycleLock = new Object();
    private volatile BeaconListener beaconListener;
    private volatile TcpClient tcpClient;
    private volatile HeartbeatMonitor heartbeatMonitor;

    public ClientNetworkRuntime(Consumer<String> statusConsumer) {
        this(statusConsumer, resolveHostname());
    }

    public ClientNetworkRuntime(Consumer<String> statusConsumer, String hostname) {
        this(statusConsumer, hostname, PortConstants.UDP_BEACON);
    }

    ClientNetworkRuntime(Consumer<String> statusConsumer, String hostname, int beaconPort) {
        this.statusConsumer = Objects.requireNonNull(statusConsumer, "statusConsumer");
        this.hostname = Objects.requireNonNull(hostname, "hostname");
        if (beaconPort < 1 || beaconPort > 0xFFFF) {
            throw new IllegalArgumentException("Beacon port must be between 1 and 65535");
        }
        this.beaconPort = beaconPort;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Client network runtime is already running");
        }
        try {
            beaconListener = new BeaconListener(beaconPort, this::onBeacon);
            beaconListener.start();
            statusConsumer.accept("Searching for the lab server...");
        } catch (IOException | RuntimeException exception) {
            running.set(false);
            if (beaconListener != null) {
                beaconListener.close();
            }
            throw exception;
        }
    }

    private void onBeacon(InetAddress address,
                          com.dau.labmanagement.shared.protocol.message.BeaconMessage beacon) {
        if (!running.get() || tcpClient != null || !connecting.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual().name("server-beacon-connect").start(() -> connectToServer(address, beacon));
    }

    private void connectToServer(
            InetAddress address,
            com.dau.labmanagement.shared.protocol.message.BeaconMessage beacon) {
        TcpClient candidate = null;
        HeartbeatMonitor candidateHeartbeat = null;
        try {
            if (beacon.protocolVersion() != ProtocolVersion.CURRENT) {
                LOG.warn("Server {} uses unsupported protocol version {}",
                        beacon.serverId(), beacon.protocolVersion());
                return;
            }

            candidate = TcpClient.connect(address, beacon.tcpPort(), Duration.ofSeconds(3));
            Socket socket = candidate.socket();
            socket.setSoTimeout(5_000);
            candidate.send(new ProtocolFrame(
                    MessageType.HELLO,
                    ProtocolVersion.CURRENT,
                    OBJECT_MAPPER.valueToTree(new HelloMessage(clientId, hostname))));
            ProtocolFrame acknowledgmentFrame = candidate.receive();
            if (acknowledgmentFrame == null
                    || acknowledgmentFrame.type() != MessageType.HELLO_ACK
                    || acknowledgmentFrame.version() != ProtocolVersion.CURRENT) {
                throw new IOException("Server did not return a valid HELLO_ACK");
            }

            HelloAckMessage acknowledgment =
                    TcpFrameCodec.payloadAs(acknowledgmentFrame, HelloAckMessage.class);
            if (acknowledgment.protocolVersion() != ProtocolVersion.CURRENT) {
                throw new IOException("Server acknowledged an unsupported protocol version");
            }

            socket.setSoTimeout(0);
            candidateHeartbeat = new HeartbeatMonitor(address, clientId, this::onConnectionLost);
            synchronized (lifecycleLock) {
                if (!running.get()) {
                    return;
                }
                candidateHeartbeat.start();
                tcpClient = candidate;
                heartbeatMonitor = candidateHeartbeat;
                candidate = null;
                candidateHeartbeat = null;
            }
            if (running.get()) {
                statusConsumer.accept("Connected to server " + acknowledgment.serverId());
            }
            LOG.info("Connected to server {} at {}", acknowledgment.serverId(), address.getHostAddress());
        } catch (IOException exception) {
            if (running.get()) {
                LOG.warn("Could not establish a session with server {}: {}",
                        address.getHostAddress(), exception.getMessage());
                statusConsumer.accept("Could not connect to server; waiting for the next beacon.");
            }
        } finally {
            closeQuietly(candidateHeartbeat);
            closeQuietly(candidate);
            connecting.set(false);
        }
    }

    private void onConnectionLost() {
        HeartbeatMonitor monitor;
        TcpClient client;
        synchronized (lifecycleLock) {
            monitor = heartbeatMonitor;
            heartbeatMonitor = null;
            client = tcpClient;
            tcpClient = null;
        }
        closeQuietly(monitor);
        closeQuietly(client);
        if (running.get()) {
            statusConsumer.accept("Server connection lost; waiting to reconnect...");
        }
    }

    /**
     * Authenticates a student over the established TCP session without sending
     * the password itself.
     *
     * @param studentId the student account ID
     * @param password the password characters; cleared before this method returns
     * @return the server's authentication result
     * @throws IOException if the TCP session or login exchange fails
     */
    public LoginAckMessage loginStudent(String studentId, char[] password) throws IOException {
        Objects.requireNonNull(password, "password");
        try {
            if (studentId == null || studentId.isBlank() || studentId.length() > 50
                    || password.length == 0) {
                throw new IllegalArgumentException("Student ID and password are required");
            }
            String normalizedStudentId = studentId.trim();
            TcpClient client = tcpClient;
            if (!running.get() || client == null) {
                throw new IOException("Client is not connected to the server");
            }

            synchronized (client) {
                if (client != tcpClient) {
                    throw new IOException("Server connection changed; please retry login");
                }
                client.send(new ProtocolFrame(
                        MessageType.LOGIN,
                        ProtocolVersion.CURRENT,
                        OBJECT_MAPPER.valueToTree(
                                new LoginMessage(normalizedStudentId, null, null))));
                LoginAckMessage challenge = readLoginAck(client);
                if (challenge.status() == LoginStatus.REJECTED) {
                    return challenge;
                }
                if (challenge.status() != LoginStatus.CHALLENGE
                        || challenge.nonce() == null || challenge.salt() == null
                        || challenge.iterations() < 1) {
                    throw new IOException("Server returned an invalid login challenge");
                }

                byte[] key = null;
                try {
                    key = PasswordProof.deriveKey(
                            password, challenge.salt(), challenge.iterations());
                    String proof = PasswordProof.createProof(key, challenge.nonce());
                    client.send(new ProtocolFrame(
                            MessageType.LOGIN,
                            ProtocolVersion.CURRENT,
                            OBJECT_MAPPER.valueToTree(new LoginMessage(
                                    normalizedStudentId, challenge.nonce(), proof))));
                } catch (GeneralSecurityException | IllegalArgumentException exception) {
                    throw new IOException("Could not create the student login proof", exception);
                } finally {
                    if (key != null) {
                        Arrays.fill(key, (byte) 0);
                    }
                }

                LoginAckMessage result = readLoginAck(client);
                if (result.status() != LoginStatus.AUTHENTICATED
                        && result.status() != LoginStatus.REJECTED) {
                    throw new IOException("Server returned an invalid login result");
                }
                return result;
            }
        } catch (IOException exception) {
            onConnectionLost();
            throw exception;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static LoginAckMessage readLoginAck(TcpClient client) throws IOException {
        ProtocolFrame frame = client.receive();
        if (frame == null || frame.type() != MessageType.LOGIN_ACK
                || frame.version() != ProtocolVersion.CURRENT) {
            throw new IOException("Server did not return a valid LOGIN_ACK");
        }
        return TcpFrameCodec.payloadAs(frame, LoginAckMessage.class);
    }

    private static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not determine this client hostname", exception);
        }
    }

    private static void closeQuietly(HeartbeatMonitor monitor) {
        if (monitor == null) {
            return;
        }
        monitor.close();
    }

    private static void closeQuietly(TcpClient client) {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (IOException exception) {
            LOG.warn("Could not close network resource", exception);
        }
    }

    @Override
    public void close() throws IOException {
        BeaconListener listener;
        HeartbeatMonitor monitor;
        TcpClient client;
        synchronized (lifecycleLock) {
            running.set(false);
            listener = beaconListener;
            beaconListener = null;
            monitor = heartbeatMonitor;
            heartbeatMonitor = null;
            client = tcpClient;
            tcpClient = null;
        }
        if (listener != null) {
            listener.close();
        }
        if (monitor != null) {
            monitor.close();
        }
        if (client != null) {
            client.close();
        }
    }
}
