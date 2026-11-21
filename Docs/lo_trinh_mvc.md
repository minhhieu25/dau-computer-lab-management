# LỘ TRÌNH THỰC HIỆN THEO MÔ HÌNH MVC

> Tài liệu dành cho thành viên nhóm khi bắt đầu code. Bám sát `Docs/ke_hoach_trien_khai.md` (8 sprint S0–S7) nhưng chi tiết hóa **thứ tự việc làm** và **cách chia tầng MVC** bên trong từng module.
> Tài liệu nguồn: `Docs/dac_tac_yeu_cau.md` (BR-01…BR-10), `Docs/phan_ra_chuc_nang.md` (7 phân hệ), `Chart/ERD/ERD.png` (9 bảng), `Chart/Workflow/` (11 sơ đồ), `Wireframe/` (13 màn hình).

---

## 1. Stack đã chốt

| Thành phần | Công cụ | Phiên bản (đã khoá trong `gradle/libs.versions.toml`) |
|---|---|---|
| Ngôn ngữ | Java | 21 LTS |
| Giao diện | JavaFX + FXML | 21.0.13 |
| Database | PostgreSQL | 16 |
| Build | Gradle 8 Kotlin DSL + Wrapper | 8.14.3 |
| Connection pool | HikariCP | 6.3.3 |
| Truy vấn | JDBI 3 | 3.55.0 |
| Migration | Flyway (+ flyway-database-postgresql) | 11.20.3 |
| JSON | Jackson | 2.23.0 |
| Mạng | TCP/UDP/Multicast **thuần JDK** | `ServerSocket/Socket`, `DatagramSocket`, `MulticastSocket` |
| Log | SLF4J + Logback | 2.0.20 / 1.5.38 |
| Test | JUnit Jupiter | 6.1.3 |
| IDE | VS Code + Extension Pack for Java | bản mới nhất |

Giai đoạn sau (theo sprint, chưa thêm vào build): JavaCV (ghi MP4 — S4), JNA + JNativeHook (chặn input — S2–S3), Zonky embedded-postgres (test DB).

---

## 2. Áp dụng MVC vào dự án này

Dự án có **2 ứng dụng JavaFX** (server, client) + 1 thư viện dùng chung. MVC được chia như sau:

| Tầng | Nằm ở đâu | Chứa gì |
|---|---|---|
| **Model** | `modules/shared` + `modules/*/src/main/java/.../model/` | DTO giao thức (Jackson records), DAO (JDBI), Service (business logic), net (TCP/UDP thuần JDK) |
| **View** | `modules/*/src/main/resources/.../view/` | FXML (mỗi màn hình 1 file), CSS (token màu theo `Docs/template.md`), custom controls |
| **Controller** | `modules/*/src/main/java/.../controller/` | JavaFX controllers (`fx:controller` trong FXML) |

**Luồng dữ liệu một chiều bắt buộc:**

```
Controller → Service → DAO (JDBI + HikariCP) → PostgreSQL 16
Controller → Service → net (TCP/UDP) → Client agent
```

**Quy tắc bất biến:**
1. Controller **không** gọi thẳng DAO, **không** đụng socket — chỉ gọi Service.
2. View (FXML/CSS) không chứa logic; controller lấy dữ liệu từ service rồi gắn vào control.
3. **Mọi enum/mã lệnh/DTO nằm duy nhất trong `modules/shared`** — server/client không tự khai báo lại.
4. DAO chỉ dịch row ↔ record; business rule nằm ở Service.
5. Mật khẩu/secret đọc từ biến môi trường trong `config/AppConfig`, không ghi trong code.

> **Ghi chú lệch tài liệu:** `Docs/ke_hoach_trien_khai.md` mục 1 ghi "MVVM nhẹ"; theo yêu cầu mới của nhóm, dự án dùng **MVC cổ điển** như trên. Nhớ cập nhật lại doc khi họp Sprint.

---

## 3. Cây thư mục chuẩn (đã scaffold xong)

```
dau-computer-lab-management/            ← repo root (git)
├── settings.gradle.kts                 # include 4 module
├── gradle.properties
├── gradle/libs.versions.toml           # version catalog — chỉ chốt version ở đây
├── gradlew / gradlew.bat + gradle/wrapper/
├── README.md
├── .gitignore
├── db/migration/                       # V1__schema.sql, V2__seed.sql (Bước 2)
├── Docs/                               # tài liệu (có sẵn) + protocol-spec.md, nfr.md, adr/ (Bước 3+)
├── Chart/  Wireframe/                  # sơ đồ, wireframe (có sẵn)
└── modules/
    ├── shared/                         # MODEL DÙNG CHUNG
    │   └── src/main/java/com/dau/labmanagement/shared/
    │       ├── protocol/               # MessageType (22 mã lệnh), PortConstants (50010/50000/50001/50020)
    │       ├── dto/                    # Jackson records — thêm từng message theo vòng đời module
    │       └── util/                   # framing, tiện ích 2 bên
    ├── server/                         # PC SERVER — MVC đầy đủ
    │   └── src/main/
    │       ├── java/com/dau/labmanagement/server/
    │       │   ├── ServerApp.java
    │       │   ├── config/             # AppConfig (env vars), Hikari, Flyway, JDBI bootstrap
    │       │   ├── controller/         # CONTROLLER — 1 controller/màn hình SC_001..SC_013
    │       │   ├── model/
    │       │   │   ├── dao/            # MODEL — 9 DAO ứng 9 bảng ERD (JDBI)
    │       │   │   ├── service/        # MODEL — LoginService, RecordingService, SubmissionService…
    │       │   │   └── net/            # MODEL — TcpServer, BeaconSender, HeartbeatReceiver, MulticastSender
    │       │   └── view/               # VIEW — ViewFactory, custom controls
    │       └── resources/com/dau/labmanagement/server/view/{fxml,css}/
    ├── client/                         # PC CLIENT — agent + MVC cho UI nhỏ
    │   └── src/main/java/com/dau/labmanagement/client/
    │       ├── ClientApp.java
    │       ├── agent/                  # HeartbeatSender, ScreenCaptureAgent, FileReceiver, InputBlocker
    │       ├── controller/  model/{service,net}/  view/
    │       └── (resources .../view/{fxml,css})
    └── shared-test/                    # fixture fake client/server cho integration test
```

---

## 4. Bước 0 — Môi trường ✅ ĐÃ XONG (21/11/2026)

Trên máy chủ already done; các thành viên khác trong nhóm lặp lại checklist này:

| Việc | Cách làm |
|---|---|
| JDK 21 Full/FX | Liberica Full 21.0.13 (zip, không cần admin) → `D:\tools\jdk-21.0.13.0.3-full`; set `JAVA_HOME`; kiểm tra `java --list-modules` có `javafx.controls` |
| PostgreSQL 16 | Cài song song port **5433** (nếu máy có 18 chiếm 5432); DB đọc từ env `DB_URL`, `DB_USER`, `DB_PASSWORD` |
| Gradle | Không cần cài CLI — dùng wrapper. (Máy đã có sẵn `D:\tools\gradle-8.14.3` để khởi tạo) |
| VS Code | Extension Pack for Java |
| Gradle Wrapper | `gradlew` + `gradle/wrapper/` đã commit trong repo, ghim 8.14.3 |

---

## 5. Bước 1 — Scaffold khung Gradle (FND-01) ✅ ĐÃ XONG

- Branch dự kiến: `feature/fnd-01-gradle-scaffold` — commit message mẫu: `feat: scaffold java solution`.
- **DoD:** `./gradlew build` xanh; `./gradlew :server:run` và `:client:run` mở được cửa sổ rỗng; cây thư mục đúng mục 3.
- 4 module + version catalog + JavaFX plugin + wrapper + `MessageType`/`PortConstants` đã có.

---

## 6. Bước 2 — Database (FND-02) — VIỆC TIẾP THEO

1. **Quyết điểm còn thiếu trước khi viết SQL:** ERD chưa có `deleted_at` cho soft-delete tài khoản (quy định ở `Docs/Work_Assignment.md`) — chốt với nhóm rồi mới migrate.
2. Viết `db/migration/V1__schema.sql` cho **9 bảng ERD** (`user`, `machine`, `system_setting`, `student_session`, `submission_session`, `recording_file`, `recording_log`, `submission`, `submission_file`):
   - Table/column snake_case; PK `pk_<table>`; FK `fk_<table>_<column>` (theo `Docs/coding_convention.md`).
3. `V2__seed.sql`: 1 tài khoản admin (mật khẩu hash PBKDF2, không seed mật khẩu trần).
4. Viết trong `modules/server` → `config/`: `AppConfig` (env vars) → `DataSourceProvider` (Hikari) → `DatabaseMigrator` (Flyway trỏ tới `db/migration`) → `JdbiProvider`.
5. Viết 9 DAO trong `model/dao/` + test bằng Zonky embedded-postgres (hoặc DB 5433 thật).
- Branch: `feature/fnd-02-db-schema` (phụ trách: Chung Cơ Tuấn).

## 7. Bước 3 — shared protocol (FND-03)

1. Viết `Docs/protocol-spec.md` theo bảng mã lệnh mục 4 của `ke_hoach_trien_khai.md` (state machine `Disconnected → Hello → LoggedIn → Busy`, timeout, fail-safe) — **artifact quyết định cho mọi sprint**.
2. Cài `TcpFrameCodec` trong `shared/util` (khung `[4B len][2B type][1B ver][JSON]`, `DataInputStream.readInt()` + `readFully`).
3. Thêm DTO records vào `shared/dto` theo từng message + unit test framing/parse.
4. Ghi `Docs/nfr.md` (≤40 máy/phòng, phản hồi ≤1s, 480p/5FPS/0,5–1 Mbps, giới hạn không chặn Ctrl+Alt+Del).
- Branch: `feature/fnd-03-network-core`.

## 8. Bước 4 — Luồng MVC dọc đầu tiên: Đăng nhập (SC_001/SC_002)

Làm 1 luồng hoàn chỉnh để cả nhóm thấy MVC chạy thật trước khi nhân rộng:

```
FXML login.fxml → LoginController → LoginService → UserDao → PostgreSQL 16 (port 5433)
Client: LoginController → TcpClient (LOGIN/LOGIN_ACK) → Server LoginService
```

Sau đó: network core (`BeaconSender`, `TcpServer` virtual-thread, `HeartbeatReceiver`, `SessionRegistry`) + `GridDashboardView` (FND-04, branch `feature/fnd-04-javafx-shell`). DoD S1: 2 PC tự kết nối <3s, rút mạng → ô Đỏ sau ~30s, dashboard đúng vai trò.

## 9. Bước 5 — Các sprint tiếp theo (S2–S7)

| Sprint | Phân hệ | BR | Workflow | Branch/SC | Người phụ trách chính |
|---|---|---|---|---|---|
| S2 | Khóa/Mở khóa | BR-02, BR-06 | `08-khoa-mo-khoa` | `feature/sc-006-lock-unlock` | Phạm Thị Thuỳ Trang (UI) |
| S3 | Trình chiếu + Focus mode | BR-03 | `04-trinh-chieu` | `feature/sc-007-projection` | Đỗ Nguyễn Hoàng Tuấn |
| S4 | Live-view + ghi hình + kho file | BR-03, BR-07, BR-10 | `06`, `07`, `03a`, `03b` | `sc-008`, `sc-009`, `sc-012`, `sc-013` | Võ Minh Hiếu |
| S5 | Phát tài liệu + Thu bài | BR-04, BR-05 | `05-phat-tai-lieu`, `09-thu-bai` | `sc-010`, `sc-011` | Chung Cơ Tuấn |
| S6 | Admin + đo lường + hardening | BR-08, BR-09 | `02-quan-ly-tai-khoan` | `feature/sc-005-account-management` | Đỗ Nguyễn Hoàng Tuấn |
| S7 | Test lab + jpackage + demo | — | — | `chore/fnd-06-integration-demo` | cả nhóm |

## 10. Vòng đời 1 module (áp dụng cho cả 9 phân hệ)

1. Thêm message + DTO vào `shared`, unit test framing/parse.
2. Viết service Server + **fake client** (console `main` trong `shared-test`) — test được trước khi có UI.
3. Viết agent Client + test fail-safe.
4. Nối JavaFX view (FXML + controller), hiển thị đúng dữ liệu Output mà module tạo ra.
5. Chạy test case tương ứng trong `test-plan.md`, cập nhật trạng thái.

## 11. Quy ước tra cứu nhanh

- **Đặt tên:** package chữ thường `com.dau.labmanagement.<module>`; class PascalCase; biến/method camelCase; hằng UPPER_SNAKE_CASE; DB snake_case; `pk_<table>`/`fk_<table>_<column>`. Chi tiết: `Docs/coding_convention.md`.
- **Comment code:** tiếng Anh, `//` một dòng, JavaDoc cho method public.
- **Git:** branch theo `Docs/Work_Assignment.md` (`feature/fnd-*`, `feature/sc-NNN-*`, `test/*`, `chore/*`); commit kiểu conventional (`feat:`, `docs:`, `chore:`); PR vào `shared` cần Hiếu review; không commit mật khẩu/secret.
- **UI:** token màu theo `Docs/template.md` (Primary #1E40AF, Online #16A34A, Unstable #D97706, Offline #DC2626; trạng thái phải có label/icon, không chỉ màu).

## 12. Việc cần làm ngay tiếp theo (theo thứ tự)

1. ✅ ~~Scaffold~~ → commit lên `feature/fnd-01-gradle-scaffold`.
2. `Docs/protocol-spec.md` + `Docs/nfr.md`.
3. `db/migration/V1__schema.sql` (9 bảng) + `V2__seed.sql` + cấu hình Hikari/Flyway/JDBI (Bước 2).
4. 4 spike 1 ngày/ý, ghi kết luận vào `Docs/adr/`: Robot 854×480 đạt bao nhiêu FPS · Multicast qua switch lab · JavaFX overlay + JNA BlockInput · JavaCV ghi mp4 từ JPEG.
5. Luồng MVC dọc đầu tiên: Đăng nhập (Bước 4).
