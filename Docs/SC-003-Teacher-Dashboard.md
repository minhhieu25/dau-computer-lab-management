# SC-003 - Teacher Dashboard

## 1. Implemented features

### Backend
- Machine session registry.
- Persistent client identity across application restarts.
- Heartbeat monitoring with a 10-second interval.
- RTT measurement using heartbeat acknowledgments.
- Packet loss estimation using a rolling window of 20 samples.
- Machine status evaluation according to BR-01.
- PostgreSQL synchronization through Jdbi DAO.
- Seat label and active student session lookup.

### Frontend
- JavaFX teacher dashboard.
- Machine grid and status counters.
- Green, yellow and red machine indicators.
- Hostname, IP address and seat label.
- Student name and student ID.
- RTT and packet loss display.
- Search and status filtering.

## 2. Machine status rules

| Status | Condition |
| --- | --- |
| Green | Heartbeat age below 10s, RTT <= 100ms, loss <= 5% |
| Yellow | Heartbeat age from 10s to below 30s, RTT > 100ms or loss > 5% |
| Red | Heartbeat age >= 30s or explicitly disconnected |

Offline status has the highest priority.

## 3. Protocol extension

HeartbeatMessage includes two additional nullable fields:
- rttMillis: Double
- packetLossPercent: Double

Metrics are reported in the next heartbeat.
Packet loss represents failed heartbeat/ACK exchanges,
not all network traffic.

Server and Client should use matching protocol builds.
Mixed-version compatibility has not been verified.

## 4. Database integration

- Existing machine and student_session tables are reused.
- No new database migration is required.
- Machine records are synchronized every 3 seconds.
- Existing seat labels are preserved.
- Student sessions older than the current machine registration
  are excluded from the dashboard.

## 5. Verification results

- Total tests: 52
- Passed: 52
- Failures: 0
- Errors: 0
- Skipped: 0

Manual verification:
- Server and Client connection.
- Machine status transitions.
- RTT and packet loss display.
- PostgreSQL synchronization.
- Seat label A01 display.

## 6. Integration notes

- Real student login requires SC-002 integration.
- Student session restoration after reconnect must be coordinated
  with SC-002.
- Persistent machine identity is scoped to the OS user.
- UDP packets from previous connections require further
  protocol-level session identification.
- Dedicated DAO integration tests remain outstanding.

## 7. Branch

feature/sc-003-teacher-dashboard
