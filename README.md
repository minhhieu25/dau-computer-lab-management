# Hệ thống Quản lý & Giám sát Phòng máy (Client-Server)

Ứng dụng desktop **Java 21 + JavaFX**: PC Server (máy giảng viên) và PC Client (máy sinh viên) giao tiếp qua **TCP/UDP thuần JDK**, dữ liệu lưu **PostgreSQL 16**. Không dùng framework backend.

## Tài liệu dự án

| Tài liệu | Nội dung |
|---|---|
| `Docs/dac_tac_yeu_cau.md` | Yêu cầu nghiệp vụ BR-01…BR-10 |
| `Docs/ke_hoach_trien_khai.md` | Stack, kiến trúc, giao thức, lộ trình 8 sprint |
| `Docs/coding_convention.md` | Quy ước đặt tên & chuẩn code |
| `Docs/lo_trinh_mvc.md` | **Lộ trình thực hiện theo MVC + cây thư mục** |
| `Chart/ERD/` · `Chart/Workflow/` · `Wireframe/` | Sơ đồ DB, workflow, wireframe 13 màn hình |

## Môi trường yêu cầu

| Thành phần | Version | Ghi chú |
|---|---|---|
| JDK | 21 LTS | **Bắt buộc bản Full/FX có JavaFX** (Liberica Full / Zulu FX). Kiểm tra: `java --list-modules` phải thấy `javafx.controls` |
| PostgreSQL | 16 | Máy dev: chạy ở **port 5433** |
| IDE | VS Code | + **Extension Pack for Java** (bắt buộc cài) |
| Gradle | 8.14.3 | Dùng qua wrapper, không cần cài CLI |

Cấu hình DB đọc từ biến môi trường (không ghi mật khẩu vào code): `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Lệnh cơ bản (chạy ở repo root)

```bash
./gradlew build          # build toàn bộ module + chạy test
./gradlew :server:run    # chạy PC Server (JavaFX)
./gradlew :client:run    # chạy PC Client (JavaFX)
./gradlew test           # chạy toàn bộ unit test
```

Windows CMD/PowerShell dùng `gradlew.bat` hoặc `.\gradlew` thay cho `./gradlew`.

## Cấu trúc module

```
modules/shared        Protocol, MessageType, DTO Jackson — duy nhất nguồn mã lệnh
modules/server        PC Server — MVC: controller / model(dao, service, net) / view
modules/client        PC Client — agent (chụp màn hình, heartbeat, nhận file) + MVC
modules/shared-test   Fixture fake client/server cho integration test
db/migration          Flyway migrations (V1__schema.sql, V2__seed.sql — Sprint 0)
```

**Quy tắc:** mọi enum/mã lệnh/DTO nằm duy nhất trong `modules/shared`; `server`/`client` không tự khai báo lại.
