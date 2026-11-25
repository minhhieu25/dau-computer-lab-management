package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.BeaconMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class BeaconListener implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BeaconListener.class);

    private final DatagramSocket socket;
    private final BiConsumer<java.net.InetAddress, BeaconMessage> beaconConsumer;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread listenerThread;

    public BeaconListener(BiConsumer<java.net.InetAddress, BeaconMessage> beaconConsumer)
            throws SocketException {
        this(PortConstants.UDP_BEACON, beaconConsumer);
    }

    public BeaconListener(int port, BiConsumer<java.net.InetAddress, BeaconMessage> beaconConsumer)
            throws SocketException {
        this.beaconConsumer = Objects.requireNonNull(beaconConsumer, "beaconConsumer");
        socket = new DatagramSocket(new InetSocketAddress(port));
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Beacon listener is already running");
        }
        listenerThread = Thread.ofVirtual().name("udp-beacon-listener").start(this::listen);
        LOG.info("UDP beacon listener listening on port {}", socket.getLocalPort());
    }

    private void listen() {
        byte[] buffer = new byte[1201];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                ProtocolFrame frame = UdpFrameCodec.decode(packet.getData(), packet.getLength());
                if (frame.type() != MessageType.BEACON || frame.version() != ProtocolVersion.CURRENT) {
                    LOG.warn("Ignoring unsupported discovery datagram from {}",
                            packet.getSocketAddress());
                    continue;
                }
                BeaconMessage beacon = UdpFrameCodec.payloadAs(frame, BeaconMessage.class);
                if (beacon.protocolVersion() != ProtocolVersion.CURRENT
                        || beacon.tcpPort() < 1 || beacon.tcpPort() > 0xFFFF) {
                    LOG.warn("Ignoring invalid discovery beacon from {}",
                            packet.getSocketAddress());
                    continue;
                }
                beaconConsumer.accept(packet.getAddress(), beacon);
            } catch (SocketException exception) {
                if (running.get()) {
                    LOG.error("UDP beacon listener socket failed", exception);
                    running.set(false);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    LOG.warn("Ignoring malformed UDP discovery datagram", exception);
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
