/**
 * MODEL layer — networking (raw JDK sockets, no external libraries).
 *
 * <p>Planned classes: {@code TcpServer} (virtual thread per connection,
 * port 50010), {@code BeaconSender} (UDP broadcast :50000 every 2 s),
 * {@code HeartbeatReceiver} (UDP :50001), {@code MulticastSender}
 * (projection stream :50020), {@code TcpFrameCodec}
 * ({@code [4B length][2B type][1B version][JSON]}).</p>
 */
package com.dau.labmanagement.server.model.net;
