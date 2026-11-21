/**
 * MODEL layer — client networking (raw JDK sockets).
 *
 * <p>Planned classes: {@code TcpClient} (connects to server :50010),
 * {@code BeaconListener} (UDP :50000 to discover the server),
 * {@code UnicastVideoSender} (JPEG frames to :50001 range),
 * {@code MulticastReceiver} (projection stream :50020),
 * {@code UdpVideoHeader} ({@code [2B frameId][2B index][2B count][JPEG]}).</p>
 */
package com.dau.labmanagement.client.model.net;
