package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

public final class TcpClient implements AutoCloseable {

    private final Socket socket;

    private TcpClient(Socket socket) {
        this.socket = socket;
    }

    public static TcpClient connect(InetAddress address, int port, Duration timeout) throws IOException {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(timeout, "timeout");
        long timeoutMillis = timeout.toMillis();
        if (timeoutMillis <= 0 || timeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Connect timeout must be between 1 ms and 2^31-1 ms");
        }

        Socket socket = new Socket();
        try {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(address, port), (int) timeoutMillis);
            return new TcpClient(socket);
        } catch (IOException exception) {
            socket.close();
            throw exception;
        }
    }

    public void send(ProtocolFrame frame) throws IOException {
        TcpFrameCodec.write(socket.getOutputStream(), frame);
    }

    public ProtocolFrame receive() throws IOException {
        return TcpFrameCodec.read(socket.getInputStream());
    }

    public Socket socket() {
        return socket;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
