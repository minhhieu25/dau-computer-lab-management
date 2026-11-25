package com.dau.labmanagement.server.network.udp;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.BeaconMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BeaconBroadcaster implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BeaconBroadcaster.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration BROADCAST_INTERVAL = Duration.ofSeconds(2);

    private final String serverId;
    private final int tcpPort;
    private final InetSocketAddress destination;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean();

    public BeaconBroadcaster(String serverId, int tcpPort) throws SocketException {
        this(serverId, tcpPort,
                new InetSocketAddress("255.255.255.255", PortConstants.UDP_BEACON));
    }

    public BeaconBroadcaster(String serverId, int tcpPort, InetSocketAddress destination) throws SocketException {
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.tcpPort = tcpPort;
        this.destination = Objects.requireNonNull(destination, "destination");
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "beacon-broadcaster");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Beacon broadcaster is already running");
        }
        scheduler.scheduleAtFixedRate(this::broadcast, 0,
                BROADCAST_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void broadcast() {
        BeaconMessage beacon = new BeaconMessage(
                serverId, tcpPort, ProtocolVersion.CURRENT, System.currentTimeMillis());
        try {
            byte[] data = UdpFrameCodec.encode(new ProtocolFrame(
                    MessageType.BEACON,
                    ProtocolVersion.CURRENT,
                    OBJECT_MAPPER.valueToTree(beacon)));
            socket.send(new DatagramPacket(data, data.length, destination));
        } catch (IOException exception) {
            if (running.get()) {
                LOG.error("Could not broadcast server discovery beacon", exception);
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        scheduler.shutdownNow();
        socket.close();
    }
}
