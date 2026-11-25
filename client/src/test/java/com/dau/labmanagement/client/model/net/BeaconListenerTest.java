package com.dau.labmanagement.client.model.net;

import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.UdpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.BeaconMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeaconListenerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void receivesAndDecodesServerBeacon() throws Exception {
        int port;
        try (DatagramSocket probe = new DatagramSocket(0)) {
            port = probe.getLocalPort();
        }

        CountDownLatch beaconReceived = new CountDownLatch(1);
        AtomicReference<BeaconMessage> receivedBeacon = new AtomicReference<>();
        try (BeaconListener listener = new BeaconListener(port, (address, beacon) -> {
            receivedBeacon.set(beacon);
            beaconReceived.countDown();
        }); DatagramSocket sender = new DatagramSocket()) {
            listener.start();
            BeaconMessage beacon = new BeaconMessage("server-1", 50010, 1, 1234);
            byte[] data = UdpFrameCodec.encode(new ProtocolFrame(
                    MessageType.BEACON,
                    ProtocolVersion.CURRENT,
                    OBJECT_MAPPER.valueToTree(beacon)));
            sender.send(new DatagramPacket(
                    data, data.length, InetAddress.getLoopbackAddress(), port));

            assertTrue(beaconReceived.await(2, TimeUnit.SECONDS));
            assertNotNull(receivedBeacon.get());
            assertEquals("server-1", receivedBeacon.get().serverId());
            assertEquals(50010, receivedBeacon.get().tcpPort());
        }
    }
}
