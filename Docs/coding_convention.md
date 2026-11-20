# Coding Convention — Hệ thống Quản lý & Giám sát Phòng máy

## 1. Môi trường & Công cụ (Environment & Tools)

| Thành phần | Công cụ / Thư viện / Framework | Phiên bản | Ghi chú / Tiêu chí chọn |
|---|---|---|---|
| Ngôn ngữ | Java | 21 LTS | Dùng thống nhất cho project |
| Giao diện | JavaFX | 21 | Xây dựng ứng dụng desktop |
| Database | PostgreSQL | 16 | Dùng chung cho nhóm |
| Build | Gradle | 8 | Kotlin DSL, dùng Gradle Wrapper |
| Database tools | Flyway, JDBI 3, HikariCP | Theo cấu hình project | Migration, truy vấn và quản lý kết nối |
| IDE | VS Code | Bản mới nhất | Dùng chung trong nhóm |
| IDE Extensions | Extension Pack for Java | Bản mới nhất | Bắt buộc cài đặt |

## 2. Quy ước đặt tên (Naming Conventions)

| Thành phần | Ngôn ngữ / Phạm vi | Quy tắc đặt tên | Ví dụ ĐÚNG | Ví dụ SAI |
|---|---|---|---|---|
| Package | Java | chữ thường | `com.dau.labmanagement.server` | `Com.Dau.LabManagement` |
| Class / Interface | Java | PascalCase | `StudentSession`, `RecordingFileDao` | `student_session` |
| Variable / Method | Java | camelCase | `machineId`, `findStudentSession()` | `Machine_id`, `find_student_session()` |
| Constant | Java | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` | `maxRetryCount` |
| Table / Column | PostgreSQL | snake_case | `student_session`, `created_at` | `StudentSession`, `createdAt` |
| Primary / Foreign Key | PostgreSQL | `pk_<table>`, `fk_<table>_<column>` | `pk_user`, `fk_student_session_user_id` | `UserPK`, `studentSessionUser` |

## 3. Comment & Cấu trúc Code (Coding Standards)

| Hạng mục | Quy định cụ thể | Ví dụ minh họa (dùng tiếng Anh) |
|---|---|---|
| Comment 1 dòng | Dùng `//`; cách một khoảng trắng; viết bằng tiếng Anh | `// Check whether the session is still active.` |
| Comment khối | Dùng khi giải thích xử lý hoặc thuật toán phức tạp | `/* Calculate the average response time. */` |
| Comment Method | Dùng JavaDoc cho method public; mô tả mục đích, parameter và return | `/** Finds a machine by its ID. */`<br>`@param machineId the machine ID`<br>`@return the machine, or null if not found` |
| Class Common / Config | Đặt cấu hình dùng chung trong class riêng; không ghi mật khẩu trực tiếp trong source code | `AppConfig` đọc cấu hình từ environment variables |
