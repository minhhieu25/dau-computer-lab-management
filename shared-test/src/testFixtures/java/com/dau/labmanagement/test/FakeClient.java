package com.dau.labmanagement.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A loopback client for integration tests; its line format is not a production protocol. */
public final class FakeClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final List<String> sentHeartbeats = new ArrayList<>();
    private final List<String> receivedCommands = new ArrayList<>();
    private final List<String> sentAcknowledgements = new ArrayList<>();

    private FakeClient(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /** Connects to a test server with finite connection and read timeouts. */
    public static FakeClient connect(InetAddress address, int port, Duration connectTimeout,
                                     Duration readTimeout) throws IOException {
        Objects.requireNonNull(address, "address");
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), timeoutMillis(connectTimeout));
            socket.setSoTimeout(timeoutMillis(readTimeout));
            return new FakeClient(socket);
        } catch (IOException | RuntimeException exception) {
            socket.close();
            throw exception;
        }
    }

    /** Sends a heartbeat payload to the paired fake server. */
    public void sendHeartbeat(String payload) throws IOException {
        String message = "HEARTBEAT " + validateLine(payload);
        send(message);
        sentHeartbeats.add(payload);
    }

    /** Waits for a command from the paired fake server, bounded by the read timeout. */
    public String receiveCommand() throws IOException {
        String message = reader.readLine();
        if (message == null) {
            return null;
        }
        if (!message.startsWith("COMMAND ")) {
            throw new IOException("Unexpected test-fixture message: " + message);
        }
        String command = message.substring("COMMAND ".length());
        receivedCommands.add(command);
        return command;
    }

    /** Sends an acknowledgement for a test command. */
    public void acknowledge(String commandId) throws IOException {
        String id = validateLine(commandId);
        send("ACK " + id);
        sentAcknowledgements.add(id);
    }

    public List<String> sentHeartbeats() {
        return List.copyOf(sentHeartbeats);
    }

    public List<String> receivedCommands() {
        return List.copyOf(receivedCommands);
    }

    public List<String> sentAcknowledgements() {
        return List.copyOf(sentAcknowledgements);
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private void send(String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
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