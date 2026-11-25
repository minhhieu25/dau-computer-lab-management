package com.dau.labmanagement.server.network;

import com.dau.labmanagement.server.network.tcp.TcpServer;
import com.dau.labmanagement.server.network.udp.BeaconBroadcaster;
import com.dau.labmanagement.server.network.udp.HeartbeatReceiver;
import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.PortConstants;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public final class ServerNetworkRuntime implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ServerNetworkRuntime.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serverId = UUID.randomUUID().toString();
    private TcpServer tcpServer;
    private HeartbeatReceiver heartbeatReceiver;
    private BeaconBroadcaster beaconBroadcaster;

    public void start() throws IOException {
        tcpServer = new TcpServer(PortConstants.TCP_SERVER, this::handleTcpConnection);
        try {
            heartbeatReceiver = new HeartbeatReceiver();
            beaconBroadcaster = new BeaconBroadcaster(serverId, tcpServer.localPort());
            tcpServer.start();
            heartbeatReceiver.start();
            beaconBroadcaster.start();
            LOG.info("Server network runtime started with id {}", serverId);
        } catch (IOException | RuntimeException exception) {
            closeAfterStartFailure(exception);
            throw exception;
        }
    }

    private void handleTcpConnection(Socket socket) throws IOException {
        socket.setSoTimeout(5_000);
        ProtocolFrame helloFrame = TcpFrameCodec.read(socket.getInputStream());
        if (helloFrame == null
                || helloFrame.type() != MessageType.HELLO
                || helloFrame.version() != ProtocolVersion.CURRENT) {
            throw new IOException("Client must begin with a version 1 HELLO frame");
        }

        HelloMessage hello = TcpFrameCodec.payloadAs(helloFrame, HelloMessage.class);
        if (hello.clientId() == null || hello.clientId().isBlank()
                || hello.hostname() == null || hello.hostname().isBlank()) {
            throw new IOException("HELLO requires clientId and hostname");
        }

        HelloAckMessage acknowledgment = new HelloAckMessage(serverId, ProtocolVersion.CURRENT);
        TcpFrameCodec.write(socket.getOutputStream(), new ProtocolFrame(
                MessageType.HELLO_ACK,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(acknowledgment)));
        socket.setSoTimeout(0);
        LOG.info("Client {} ({}) established a TCP session from {}",
                hello.clientId(), hello.hostname(), socket.getRemoteSocketAddress());

        while (TcpFrameCodec.read(socket.getInputStream()) != null) {
            LOG.debug("Received a TCP protocol frame from client {}", hello.clientId());
        }
    }

    private void closeAfterStartFailure(Exception failure) {
        try {
            close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    @Override
    public void close() throws IOException {
        IOException closeFailure = null;
        if (beaconBroadcaster != null) {
            beaconBroadcaster.close();
        }
        if (heartbeatReceiver != null) {
            heartbeatReceiver.close();
        }
        if (tcpServer != null) {
            try {
                tcpServer.close();
            } catch (IOException exception) {
                closeFailure = exception;
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }
}
