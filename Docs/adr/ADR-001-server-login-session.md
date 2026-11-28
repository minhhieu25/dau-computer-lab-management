# ADR-001 — Phiên đăng nhập Server lưu trong bộ nhớ

- **Trạng thái:** Đã chấp nhận (SC_001, Sprint 1)
- **Ngày:** 27/11/2026
- **Liên quan:** BR-09, SC_001, `feature/sc-001-server-login`

## Bối cảnh

Phần mềm Server yêu cầu đăng nhập trước khi điều khiển lớp học (BR-09). ERD có
bảng `student_session` cho sinh viên đăng nhập trên Client (SC_002) nhưng **không
có bảng nào cho phiên đăng nhập của Giảng viên/Admin trên Server**.

## Quyết định

Phiên đăng nhập Server được giữ **trong bộ nhớ** bởi
`LoginSessionRegistry` (`server/model/service`), không thêm bảng vào schema:

- `LoginSession` gồm `sessionId` (UUID), `userId`, `username`, `fullName`,
  `role` (ADMIN | TEACHER) và `loginAt`.
- `JdbiLoginService` mở phiên sau khi xác thực thành công; `ServerApp` giữ một
  registry duy nhất cho vòng đời tiến trình và gọi `closeAll()` khi thoát.
- Không ghi mật khẩu, không ghi hash vào phiên; mảng `char[]` mật khẩu bị xoá
  ngay sau khi kiểm tra.

## Lý do

1. Phiên Server chỉ có ý nghĩa với tiến trình đang chạy: tắt Server thì phiên
   phải mất, nên không cần bền vững.
2. Tránh sửa schema V1 khi chưa họp chốt ERD (SC_005 đang chờ quyết định
   xóa mềm/xóa cứng).
3. Không thêm migration chỉ để phục vụ trạng thái tạm thời.

## Hệ quả

- Nhiều cửa sổ Server trên cùng máy dùng chung registry; đăng xuất ở một cửa sổ
  chỉ đóng phiên của chính nó.
- Khi cần audit đăng nhập Server (S6), thêm bảng `server_login_log` bằng
  migration mới thay vì đổi quyết định này.
- Test `JdbiLoginServiceIT` chạy trên PostgreSQL nhúng nên vẫn kiểm chứng được
  toàn bộ chuỗi DAO → service → registry.
