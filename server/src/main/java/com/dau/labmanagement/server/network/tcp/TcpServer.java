package com.dau.labmanagement.server.network.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;

public final class TcpServer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);

    private final ServerSocket serverSocket;
    private final TcpConnectionHandler connectionHandler;
    private final ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread acceptThread;

    public TcpServer(int port, TcpConnectionHandler connectionHandler) throws IOException {
        this(new InetSocketAddress(port), connectionHandler);
    }

    public TcpServer(InetSocketAddress bindAddress, TcpConnectionHandler connectionHandler) throws IOException {
        this.connectionHandler = Objects.requireNonNull(connectionHandler, "connectionHandler");
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(bindAddress);
    }

    public int localPort() {
        return serverSocket.getLocalPort();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("TCP server is already running");
        }
        acceptThread = Thread.ofVirtual().name("tcp-server-accept").start(this::acceptConnections);
        LOG.info("TCP server listening on port {}", localPort());
    }

    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                clients.add(socket);
                clientExecutor.submit(() -> handleConnection(socket));
            } catch (SocketException exception) {
                if (running.get()) {
                    LOG.error("TCP accept failed", exception);
                    running.set(false);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    LOG.error("TCP accept failed", exception);
                    running.set(false);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket) {
            connectionHandler.handle(socket);
        } catch (IOException exception) {
            if (running.get()) {
                LOG.warn("TCP client {} disconnected with an I/O error",
                        socket.getRemoteSocketAddress(), exception);
            }
        } finally {
            clients.remove(socket);
        }
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        IOException closeFailure = null;
        try {
            serverSocket.close();
        } catch (IOException exception) {
            closeFailure = exception;
        }
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException exception) {
                if (closeFailure == null) {
                    closeFailure = exception;
                } else {
                    closeFailure.addSuppressed(exception);
                }
            }
        }
        clientExecutor.shutdownNow();
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }
}
