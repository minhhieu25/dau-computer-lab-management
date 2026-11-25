/**
 * Client agent — background behavior that is not a user interface.
 *
 * <p>Planned classes: {@code HeartbeatSender} (UDP :50001, 10 s period),
 * {@code ScreenCaptureAgent} ({@code java.awt.Robot}, JPEG 854x480, 5 FPS),
 * {@code FileReceiver} (writes to D:\ThuMucDungChung\&lt;MSSV&gt;\DeBai),
 * {@code InputBlocker} (JNA), {@code FocusMode}, {@code OverlayStage}.
 * The agent must always run the fail-safe rule: after 3 lost ACKs (~30 s)
 * unlock the screen, exit focus mode and stop all streams (BR-06).</p>
 */
package com.dau.labmanagement.client.agent;
