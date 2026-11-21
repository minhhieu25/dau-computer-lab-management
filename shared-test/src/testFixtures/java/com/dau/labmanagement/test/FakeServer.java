package com.dau.labmanagement.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A loopback server for integration tests; its line format is not a production protocol. */
public final class FakeServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final int readTimeoutMillis;
    private final List<String> receivedHeartbeats = new ArrayList<>();
    private final List<String> sentCommands = new ArrayList<>();
    private final List<String> receivedAcknowledgements = new ArrayList<>();
    private Socket clientSocket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private FakeServer(ServerSocket serverSocket, int readTimeoutMillis) {
        this.serverSocket = serverSocket;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /** Binds an ephemeral TCP port on loopback and configures finite timeouts. */
    public static FakeServer start(Duration timeout) throws IOException {
        int timeoutMillis = timeoutMillis(timeout);
        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        serverSocket.setSoTimeout(timeoutMillis);
        return new FakeServer(serverSocket, timeoutMillis);
    }

    /** Accepts one test client, bounded by the configured timeout. */
    public void acceptClient() throws IOException {
        if (clientSocket != null) {
            throw new IllegalStateException("A test client has already been accepted");
        }
        clientSocket = serverSocket.accept();
        clientSocket.setSoTimeout(readTimeoutMillis);
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /** Sends a test command to the connected client. */
    public void sendCommand(String command) throws IOException {
        String value = validateLine(command);
        send("COMMAND " + value);
        sentCommands.add(value);
    }

    /** Receives a heartbeat payload, or returns null when the client disconnects. */
    public String receiveHeartbeat() throws IOException {
        String message = readMessage("HEARTBEAT ");
        if (message != null) {
            receivedHeartbeats.add(message);
        }
        return message;
    }

    /** Receives and records an acknowledgement, or returns null when the client disconnects. */
    public String receiveAcknowledgement() throws IOException {
        String message = readMessage("ACK ");
        if (message != null) {
            receivedAcknowledgements.add(message);
        }
        return message;
    }

    /** Closes the accepted client socket to simulate a server-side connection loss. */
    public void disconnectClient() throws IOException {
        if (clientSocket != null) {
            clientSocket.close();
        }
    }

    public List<String> receivedHeartbeats() {
        return List.copyOf(receivedHeartbeats);
    }

    public List<String> sentCommands() {
        return List.copyOf(sentCommands);
    }

    public List<String> receivedAcknowledgements() {
        return List.copyOf(receivedAcknowledgements);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public boolean isClosed() {
        return serverSocket.isClosed();
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            disconnectClient();
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            serverSocket.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private String readMessage(String prefix) throws IOException {
        requireClient();
        String message = reader.readLine();
        if (message == null) {
            return null;
        }
        if (!message.startsWith(prefix)) {
            throw new IOException("Unexpected test-fixture message: " + message);
        }
        return message.substring(prefix.length());
    }

    private void send(String message) throws IOException {
        requireClient();
        writer.write(message);
        writer.newLine();
        writer.flush();
    }

    private void requireClient() {
        if (clientSocket == null || clientSocket.isClosed()) {
            throw new IllegalStateException("No connected test client");
        }
    }

    private static String validateLine(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Payload must be a non-blank single line");
        }
        return value;
    }

    private static int timeoutMillis(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        long millis = timeout.toMillis();
        if (millis < 1 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout must be between 1 ms and Integer.MAX_VALUE ms");
        }
        return (int) millis;
    }
}