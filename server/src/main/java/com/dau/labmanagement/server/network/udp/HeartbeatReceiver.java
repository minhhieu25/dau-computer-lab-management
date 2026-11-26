package com.dau.labmanagement.server.network.udp;

import com.dau.labmanagement.server.model.dao.MachineDao;
import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.AckMessage;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HeartbeatReceiver implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatReceiver.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Jdbi jdbi;
    private final DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread receiverThread;

    public HeartbeatReceiver(Jdbi jdbi) throws SocketException {
        this(jdbi, new InetSocketAddress(PortConstants.UDP_HEARTBEAT));
    }

    public HeartbeatReceiver(Jdbi jdbi, InetSocketAddress bindAddress) throws SocketException {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi");
        socket = new DatagramSocket(bindAddress);
    }

    public int localPort() {
        return socket.getLocalPort();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Heartbeat receiver is already running");
        }
        receiverThread = Thread.ofVirtual().name("udp-heartbeat-receiver").start(this::receiveHeartbeats);
        LOG.info("UDP heartbeat receiver listening on port {}", localPort());
    }

    private void receiveHeartbeats() {
        byte[] buffer = new byte[1201];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                ProtocolFrame frame = UdpFrameCodec.decode(packet.getData(), packet.getLength());
                if (frame.type() != MessageType.HEARTBEAT || frame.version() != ProtocolVersion.CURRENT) {
                    LOG.warn("Ignoring unsupported UDP heartbeat frame from {}",
                            packet.getSocketAddress());
                    continue;
                }

                HeartbeatMessage heartbeat = UdpFrameCodec.payloadAs(frame, HeartbeatMessage.class);
                UUID machineGuid;
                try {
                    machineGuid = UUID.fromString(heartbeat.clientId());
                } catch (IllegalArgumentException | NullPointerException exception) {
                    LOG.warn("Ignoring heartbeat with invalid client ID from {}",
                            packet.getSocketAddress());
                    continue;
                }
                int updated = jdbi.withExtension(MachineDao.class,
                        dao -> dao.updateLastSeen(machineGuid, OffsetDateTime.now()));
                if (updated == 0) {
                    LOG.debug("Ignoring heartbeat from unregistered client {}", machineGuid);
                    continue;
                }

                AckMessage acknowledgment = new AckMessage(
                        heartbeat.clientId(), heartbeat.sequence(), System.currentTimeMillis());
                byte[] response = UdpFrameCodec.encode(new ProtocolFrame(
                        MessageType.ACK,
                        ProtocolVersion.CURRENT,
                        OBJECT_MAPPER.valueToTree(acknowledgment)));
                socket.send(new DatagramPacket(
                        response, response.length, packet.getSocketAddress()));
            } catch (SocketException exception) {
                if (running.get()) {
                    LOG.error("UDP heartbeat receiver socket failed", exception);
                    running.set(false);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    LOG.warn("Ignoring malformed UDP heartbeat datagram", exception);
                }
            } catch (RuntimeException exception) {
                if (running.get()) {
                    LOG.error("Could not update machine heartbeat state", exception);
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
    }
}
