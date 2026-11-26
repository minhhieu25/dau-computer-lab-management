package com.dau.labmanagement.server.network;

import com.dau.labmanagement.server.network.tcp.ClientSessionHandler;
import com.dau.labmanagement.server.network.tcp.TcpServer;
import com.dau.labmanagement.server.network.udp.BeaconBroadcaster;
import com.dau.labmanagement.server.network.udp.HeartbeatReceiver;
import com.dau.labmanagement.shared.protocol.PortConstants;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class ServerNetworkRuntime implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ServerNetworkRuntime.class);
    private final String serverId = UUID.randomUUID().toString();
    private final Jdbi jdbi;
    private TcpServer tcpServer;
    private HeartbeatReceiver heartbeatReceiver;
    private BeaconBroadcaster beaconBroadcaster;

    public ServerNetworkRuntime(Jdbi jdbi) {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi");
    }

    public void start() throws IOException {
        tcpServer = new TcpServer(
                PortConstants.TCP_SERVER, new ClientSessionHandler(jdbi, serverId));
        try {
            heartbeatReceiver = new HeartbeatReceiver(jdbi);
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
