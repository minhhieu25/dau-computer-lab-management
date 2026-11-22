package com.dau.labmanagement.server.network.udp;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.AckMessage;
import com.dau.labmanagement.shared.protocol.message.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatReceiverTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void returnsAnAckToTheHeartbeatSource() throws Exception {
        try (HeartbeatReceiver receiver = new HeartbeatReceiver(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
             DatagramSocket client = new DatagramSocket()) {
            receiver.start();
            client.setSoTimeout((int) Duration.ofSeconds(2).toMillis());

            byte[] request = UdpFrameCodec.encode(new ProtocolFrame(
                    MessageType.HEARTBEAT,
                    ProtocolVersion.CURRENT,
                    OBJECT_MAPPER.valueToTree(new HeartbeatMessage("client-1", 7, 1000))));
            client.send(new DatagramPacket(
                    request, request.length, InetAddress.getLoopbackAddress(), receiver.localPort()));

            byte[] responseBuffer = new byte[1201];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            client.receive(response);
            ProtocolFrame frame = UdpFrameCodec.decode(response.getData(), response.getLength());

            assertEquals(MessageType.ACK, frame.type());
            AckMessage acknowledgment = UdpFrameCodec.payloadAs(frame, AckMessage.class);
            assertEquals("client-1", acknowledgment.clientId());
            assertEquals(7, acknowledgment.sequence());
            assertTrue(acknowledgment.serverReceivedAtEpochMillis() > 0);
        }
    }
}
