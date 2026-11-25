package com.dau.labmanagement.server.network.tcp;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TcpServerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void acceptsAFramedHelloAndReturnsHelloAck() throws Exception {
        AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
        try (TcpServer server = new TcpServer(0, socket -> {
            try (socket) {
                ProtocolFrame request = TcpFrameCodec.read(socket.getInputStream());
                assertEquals(MessageType.HELLO, request.type());
                TcpFrameCodec.write(
                        socket.getOutputStream(),
                        new ProtocolFrame(
                                MessageType.HELLO_ACK,
                                ProtocolVersion.CURRENT,
                                OBJECT_MAPPER.valueToTree(new HelloAckMessage("server-1", 1))));
            } catch (Throwable failure) {
                handlerFailure.set(failure);
            }
        })) {
            server.start();
            try (Socket client = new Socket()) {
                client.connect(new java.net.InetSocketAddress(
                        InetAddress.getLoopbackAddress(), server.localPort()), 2_000);
                TcpFrameCodec.write(client.getOutputStream(), new ProtocolFrame(
                        MessageType.HELLO,
                        ProtocolVersion.CURRENT,
                        OBJECT_MAPPER.valueToTree(new HelloMessage("client-1", "lab-pc-01"))));
                ProtocolFrame response = TcpFrameCodec.read(client.getInputStream());
                assertEquals(MessageType.HELLO_ACK, response.type());
                assertEquals(new HelloAckMessage("server-1", 1),
                        com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec
                                .payloadAs(response, HelloAckMessage.class));
            }
            if (handlerFailure.get() != null) {
                throw new AssertionError("TCP connection handler failed", handlerFailure.get());
            }
        }
    }
}
