# Protocol Specification — Version 1

This document defines the first shared wire contract for the Server and Client.
Message codes and frame encoding are implemented in `shared`.

## 1. Common rules

- Protocol version: `1`.
- Integer fields in binary headers use network byte order (big-endian).
- JSON payloads are UTF-8 JSON objects. Empty, malformed, or non-object payloads
  are invalid.
- TCP connections are reliable and ordered. Unknown message codes, unsupported
  protocol versions, invalid JSON, and invalid frame lengths are protocol errors;
  the receiver closes that connection.
- A receiver must not allocate a TCP frame larger than 1 MiB or accept a UDP
  protocol datagram larger than 1,200 bytes.
- Codes are stable identifiers. They must not be derived from enum ordinals or
  enum names.

## 2. TCP frame

```text
+----------------------+----------------------+-------------------+------------------+
| 4-byte body length   | 2-byte message code   | 1-byte version    | JSON payload     |
+----------------------+----------------------+-------------------+------------------+
```

`body length` counts every byte after the four-byte length field, including the
two-byte message code, the version byte, and the payload. Its minimum is 3
bytes and its maximum is 1,048,576 bytes. The JSON payload is read completely
before a frame is dispatched; TCP packet boundaries have no protocol meaning.

## 3. UDP control frame

```text
+----------------------+-------------------+------------------+
| 2-byte message code  | 1-byte version    | JSON payload     |
+----------------------+-------------------+------------------+
```

UDP frames omit the TCP length prefix because the datagram itself provides the
boundary. Datagrams over 1,200 bytes are rejected to avoid IP fragmentation.
Video fragmentation has its own frame format and is outside this control-frame
codec.

## 4. Message codes

| Code | Message |
|---:|---|
| 1 | `BEACON` |
| 2 | `HELLO` |
| 3 | `HELLO_ACK` |
| 4 | `LOGIN` |
| 5 | `LOGIN_ACK` |
| 6 | `HEARTBEAT` |
| 7 | `ACK` |
| 8 | `LOCK` |
| 9 | `UNLOCK` |
| 10 | `PROJ_START` |
| 11 | `PROJ_STOP` |
| 12 | `MON_START` |
| 13 | `MON_STOP` |
| 14 | `REC_START` |
| 15 | `REC_STOP` |
| 16 | `FILE_BEGIN` |
| 17 | `FILE_CHUNK` |
| 18 | `FILE_END` |
| 19 | `COLLECT_START` |
| 20 | `METRIC` |
| 21 | `LOSS_REPORT` |
| 22 | `RESULT` |

`shared.protocol.MessageType` is the authoritative code-to-name mapping.

## 5. Discovery and session establishment

| Message | Transport | Direction | Payload |
|---|---|---|---|
| `BEACON` | UDP broadcast, port 50000 | Server → LAN | `serverId`, `tcpPort`, `protocolVersion`, `sentAtEpochMillis` |
| `HELLO` | TCP, port 50010 | Client → Server | `clientId`, `hostname` |
| `HELLO_ACK` | TCP | Server → Client | `serverId`, `protocolVersion` |

The Server broadcasts a beacon every 2 seconds. A Client opens a TCP connection
to the advertised address and port and sends `HELLO` as its first frame. The
Server replies with `HELLO_ACK`; a version mismatch or invalid first message
causes the connection to be closed. Login is a later exchange and does not
change the meaning of this handshake.

## 6. Heartbeat

| Message | Transport | Direction | Payload |
|---|---|---|---|
| `HEARTBEAT` | UDP unicast, destination port 50001 | Client → Server | `clientId`, `sequence`, `sentAtEpochMillis` |
| `ACK` | UDP unicast to the request source address/port | Server → Client | `clientId`, `sequence`, `serverReceivedAtEpochMillis` |

The Client sends a heartbeat every 10 seconds. The Server echoes the sequence
in the acknowledgment. A Client treats an ACK with a different client ID or
sequence as stale and ignores it. Three consecutive missed acknowledgments
mark the connection as lost; higher-level Client features must then unlock any
local controls, exit focus mode, and stop active streams. This transport layer
reports loss but does not implement those feature-specific actions.

## 7. Ports

| Purpose | Port | Transport |
|---|---:|---|
| Command and data connection | 50010 | TCP |
| LAN discovery beacon | 50000 | UDP |
| Heartbeat | 50001 | UDP |
| Projection multicast | 50020 | UDP |

## 8. Scope and compatibility

Version 1 implements stable message codes, typed DTOs for discovery/session/
heartbeat, TCP framing, and UDP control framing. Login, classroom control, file
transfer, projection video, and screen streaming use the reserved codes above
but are implemented by their respective feature tasks.
