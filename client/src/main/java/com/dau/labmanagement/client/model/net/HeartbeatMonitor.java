package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.AckMessage;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HeartbeatMonitor implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatMonitor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MISSED_ACK_LIMIT = 3;

    private final InetSocketAddress serverAddress;
    private final String clientId;
    private final Duration interval;
    private final Duration acknowledgmentTimeout;
    private final Runnable connectionLostHandler;
    private final DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread monitorThread;

    public HeartbeatMonitor(InetAddress serverAddress, String clientId, Runnable connectionLostHandler)
            throws IOException {
        this(new InetSocketAddress(serverAddress, PortConstants.UDP_HEARTBEAT),
                clientId, Duration.ofSeconds(10), Duration.ofSeconds(3), connectionLostHandler);
    }

    public HeartbeatMonitor(InetSocketAddress serverAddress, String clientId,
                            Duration interval, Duration acknowledgmentTimeout,
                            Runnable connectionLostHandler) throws IOException {
        this.serverAddress = Objects.requireNonNull(serverAddress, "serverAddress");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.interval = requirePositive(interval, "interval");
        this.acknowledgmentTimeout = requirePositive(acknowledgmentTimeout, "acknowledgmentTimeout");
        this.connectionLostHandler = Objects.requireNonNull(connectionLostHandler, "connectionLostHandler");
        socket = new DatagramSocket();
        socket.setSoTimeout(Math.toIntExact(acknowledgmentTimeout.toMillis()));
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Heartbeat monitor is already running");
        }
        monitorThread = Thread.ofVirtual().name("udp-heartbeat-monitor").start(this::monitor);
    }

    private void monitor() {
        long sequence = 0;
        int missedAcknowledgments = 0;
        while (running.get()) {
            long cycleStarted = System.nanoTime();
            sequence++;
            try {
                sendHeartbeat(sequence);
                if (receiveAcknowledgment(sequence)) {
                    missedAcknowledgments = 0;
                } else {
                    missedAcknowledgments++;
                    LOG.warn("Missed heartbeat acknowledgment {}/{}",
                            missedAcknowledgments, MISSED_ACK_LIMIT);
                    if (missedAcknowledgments >= MISSED_ACK_LIMIT) {
                        running.set(false);
                        connectionLostHandler.run();
                        break;
                    }
                }
                sleepUntilNextCycle(cycleStarted);
            } catch (IOException exception) {
                if (running.get()) {
                    LOG.error("Heartbeat exchange failed", exception);
                    running.set(false);
                    connectionLostHandler.run();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                if (running.get()) {
                    LOG.error("Heartbeat monitor was interrupted unexpectedly", exception);
                    running.set(false);
                    connectionLostHandler.run();
                }
            }
        }
    }

    private void sendHeartbeat(long sequence) throws IOException {
        HeartbeatMessage heartbeat = new HeartbeatMessage(
                clientId, sequence, System.currentTimeMillis());
        byte[] data = UdpFrameCodec.encode(new ProtocolFrame(
                MessageType.HEARTBEAT,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(heartbeat)));
        socket.send(new DatagramPacket(data, data.length, serverAddress));
    }

    private boolean receiveAcknowledgment(long sequence) throws IOException {
        byte[] buffer = new byte[1201];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException exception) {
            return false;
        }

        if (!serverAddress.getAddress().equals(packet.getAddress())
                || serverAddress.getPort() != packet.getPort()) {
            return false;
        }
        ProtocolFrame frame = UdpFrameCodec.decode(packet.getData(), packet.getLength());
        if (frame.type() != MessageType.ACK || frame.version() != ProtocolVersion.CURRENT) {
            return false;
        }
        AckMessage acknowledgment = UdpFrameCodec.payloadAs(frame, AckMessage.class);
        return clientId.equals(acknowledgment.clientId()) && sequence == acknowledgment.sequence();
    }

    private void sleepUntilNextCycle(long cycleStarted) throws InterruptedException {
        long intervalNanos = interval.toNanos();
        long elapsedNanos = System.nanoTime() - cycleStarted;
        long remainingNanos = intervalNanos - elapsedNanos;
        if (remainingNanos > 0) {
            long millis = remainingNanos / 1_000_000;
            int nanos = (int) (remainingNanos % 1_000_000);
            Thread.sleep(millis, nanos);
        }
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()
                || duration.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " must be a positive duration representable in milliseconds");
        }
        return duration;
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }
}
