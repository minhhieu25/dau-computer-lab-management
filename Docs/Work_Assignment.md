# PHÂN CÔNG CÔNG VIỆC — HỆ THỐNG QUẢN LÝ & GIÁM SÁT PHÒNG MÁY

## 1. Cơ sở phân tích

- Dự án là ứng dụng desktop Client–Server cho phòng máy, không phải website.
- Công nghệ theo kế hoạch hiện tại: Java 21, JavaFX 21, PostgreSQL 16, Gradle 8; Flyway, JDBI 3 và HikariCP cho dữ liệu.
- Kiến trúc dự kiến có `shared`, `server`, `client`; các luồng mạng dùng TCP, UDP Unicast và UDP Multicast theo yêu cầu.
- Danh sách màn hình được suy ra từ BRD, ERD, BFD và các workflow trong `Chart/Workflow/`. ID `SC_` là mã đề xuất để theo dõi công việc, không phải mã Figma đã có.
- Repository hiện có tài liệu và sơ đồ, chưa có source Java, Gradle scaffold hoặc migration database. Vì vậy các đầu việc nền tảng phải hoàn thành trước khi tích hợp màn hình.
- Nhóm gồm 5 thành viên; Võ Minh Hiếu là trưởng nhóm, phụ trách nền tảng protocol/network và điều phối tích hợp. Không phân công cho thành viên ngoài danh sách.
- Âu Thị Huyền Trang phụ trách test plan và điều phối kiểm thử tích hợp; thành viên sở hữu màn hình vẫn chịu trách nhiệm test chức năng của màn hình đó.

Tài liệu tham chiếu: [Đặc tả yêu cầu](./dac_tac_yeu_cau.md), [Phân rã chức năng](./phan_ra_chuc_nang.md), [Kế hoạch triển khai](./ke_hoach_trien_khai.md), [ERD](../Chart/ERD/ERD.png), [Workflow](../Chart/Workflow/README.md).

## 2. Công việc nền tảng dùng chung

| ID | Đầu việc | Thành viên phụ trách | Kết quả cần bàn giao | Dependencies / Order | Git Branch |
|---|---|---|---|---|---|
| FND-01 | Gradle và cấu trúc module | Chung Cơ Tuấn | Gradle Wrapper, Java 21 toolchain, module `shared`, `server`, `client`, cấu hình kết nối lấy từ môi trường | Bước đầu tiên | `feature/fnd-01-gradle-scaffold` |
| FND-02 | Schema PostgreSQL và migration đầu tiên | Chung Cơ Tuấn | Flyway migration cho 9 thực thể trong ERD, khóa/quan hệ/index cơ bản; Java mapping thống nhất | Sau FND-01 | `feature/fnd-02-db-schema` |
| FND-03 | Hợp đồng protocol và nền tảng mạng | Võ Minh Hiếu (Trưởng nhóm) | Message/DTO trong `shared`, framing TCP, beacon, heartbeat/ACK và nền tảng UDP; ghi rõ timeout, lỗi và phiên bản protocol | Sau FND-01; song song FND-02 | `feature/fnd-03-network-core` |
| FND-04 | JavaFX shell và điều hướng theo vai trò | Phạm Thị Thuỳ Trang | Khung ứng dụng, điều hướng Server, Client và các vùng màn hình theo quyền; áp dụng token trong `Docs/template.md` | Sau FND-01; song song FND-02/FND-03 | `feature/fnd-04-javafx-shell` |
| FND-05 | Kế hoạch kiểm thử và test harness | Âu Thị Huyền Trang | Test plan theo BR, JUnit 5 và Zonky embedded-postgres cho kiểm thử DAO | Test plan sau FND-01; kiểm thử DAO sau FND-02 | `test/fnd-05-test-baseline` |
| FND-06 | Điều phối tích hợp và demo | Võ Minh Hiếu (Trưởng nhóm) | Theo dõi phụ thuộc, điều phối review/merge, xử lý xung đột tích hợp, chuẩn bị build tích hợp và demo | Xuyên suốt; bắt đầu sau FND-01, tích hợp theo từng màn hình | `chore/fnd-06-integration-demo` |

## 3. Bảng phân công theo màn hình

| Screen ID | Screen Name | Module | Assigned Member | Back-End Tasks (Java + PostgreSQL) | Front-End Tasks (JavaFX) | Dependencies / Order | Git Branch |
|---|---|---|---|---|---|---|---|
| SC_001 | Đăng nhập Server | Auth | Chung Cơ Tuấn | Kiểm tra tài khoản, password hash, trạng thái và role; tạo phiên đăng nhập; từ chối tài khoản không hợp lệ | Form đăng nhập, báo lỗi rõ ràng, điều hướng Admin/Giảng viên theo role | FND-01, FND-02; làm trước màn hình theo role | `feature/sc-001-server-login` |
| SC_002 | Đăng nhập định danh Client | Student Identity | Âu Thị Huyền Trang | Xác thực tài khoản Sinh viên, gắn người dùng với máy và phiên; máy vẫn Online khi chưa đăng nhập | Form MSSV/mật khẩu trên Client; hiển thị danh tính hoặc lỗi xác thực | FND-01, FND-02, FND-03; dùng chung hợp đồng xác thực với SC_001 | `feature/sc-002-client-login` |
| SC_003 | Dashboard phòng máy (Giảng viên) | Connection & Monitoring | Đỗ Nguyễn Hoàng Tuấn | Quản lý phiên máy, heartbeat, RTT, packet loss và trạng thái Xanh/Vàng/Đỏ | Lưới máy, trạng thái, người dùng/ghế máy, RTT và mất gói | FND-02, FND-03, FND-04, SC_001; ưu tiên hoàn thành sớm để các màn hình điều khiển dùng chọn máy | `feature/sc-003-teacher-dashboard` |
| SC_004 | Dashboard quản trị | Admin | Phạm Thị Thuỳ Trang | Trả danh sách chức năng được phép theo role; không cấp quyền điều khiển lớp cho Admin | Màn hình điều hướng tới Quản lý tài khoản và Quản lý file ghi hình | FND-04, SC_001; song song với SC_003 | `feature/sc-004-admin-dashboard` |
| SC_005 | Quản lý tài khoản | Account Management | Chung Cơ Tuấn | Liệt kê/tạo tài khoản, khóa/mở khóa, đổi mật khẩu, xóa; kiểm tra role và trạng thái | Bảng tài khoản, form tạo, thao tác khóa, đặt lại mật khẩu và xóa | FND-02, SC_001, SC_004; chốt cách xóa mềm/xóa cứng trước khi sửa schema vì ERD chưa có cột xóa mềm | `feature/sc-005-account-management` |
| SC_006 | Khóa / Mở khóa màn hình | Classroom Control | Phạm Thị Thuỳ Trang | Phát lệnh có `commandId`, chống xử lý lặp, đồng bộ máy kết nối muộn và tổng hợp tiến độ | Chọn máy, nhập thông báo, xác nhận lệnh và theo dõi tiến độ; Client hiển thị overlay và trạng thái mở khóa | FND-03, SC_003; Client phải có fail-safe khi mất ACK hoặc TCP | `feature/sc-006-lock-unlock` |
| SC_007 | Trình chiếu bài giảng | Projection | Phạm Thị Thuỳ Trang | Điều khiển bắt đầu/dừng; phát luồng UDP Multicast; nhận báo cáo mất gói từ Client | Điều khiển trình chiếu; Client tham gia/rời multicast và hiển thị bài giảng trong Focus Mode | FND-03, SC_003; cần kiểm thử Multicast trên mạng phòng máy | `feature/sc-007-projection` |
| SC_008 | Giám sát màn hình sinh viên | Live-view | Võ Minh Hiếu | Nhận và ghép gói UDP Unicast; đo FPS, băng thông, mất gói; chỉ nhận luồng từ máy được chọn | Chọn máy, xem live-view; Client hiển thị cảnh báo đang bị giám sát | FND-03, SC_003; hoàn thành trước SC_009 | `feature/sc-008-live-view` |
| SC_009 | Ghi hình live-view | Recording | Võ Minh Hiếu | Ghi MP4 480p/5 FPS, lưu metadata vào `RecordingFile`, đặt `expires_at`; ghi nhật ký thao tác | Nút bắt đầu/dừng ghi; cập nhật trạng thái và cảnh báo ghi hình trên Client | SC_008, FND-02; phụ thuộc luồng live-view, không phải chức năng độc lập trên Dashboard | `feature/sc-009-recording` |
| SC_010 | Phát tài liệu (Push) | File Distribution | Âu Thị Huyền Trang | Gửi file qua TCP tới máy được chọn; theo dõi byte đã gửi, tốc độ, phần trăm và ETA | Chọn file/máy, xem tiến độ từng máy; Client lưu vào thư mục `DeBai` theo MSSV | FND-03, SC_002, SC_003; chỉ gửi tới sinh viên đã đăng nhập | `feature/sc-010-file-distribution` |
| SC_011 | Thu bài (Pull) | Submission | Chung Cơ Tuấn | Tạo phiên thu, nhận file, lưu theo giảng viên/đợt/IP/MSSV; cập nhật đã nộp/chưa nộp và tiến độ | Chọn máy, bắt đầu thu, xem trạng thái và tiến độ; Client gửi nội dung thư mục `NopBai` | FND-02, FND-03, SC_002, SC_003; dùng chung nền tảng truyền file với SC_010 | `feature/sc-011-submission` |
| SC_012 | Kho ghi hình của Giảng viên | Recording Library | Phạm Thị Thuỳ Trang | Truy vấn file do chính Giảng viên hiện tại ghi; áp dụng phân quyền khi lấy metadata/file | Danh sách, thông tin và phát lại file được phép truy cập | SC_009, SC_001; phải xác minh không hiển thị file của Giảng viên khác | `feature/sc-012-teacher-recordings` |
| SC_013 | Quản lý ghi hình và nhật ký (Admin) | Recording Administration | Chung Cơ Tuấn | Liệt kê mọi file, xóa file, cấu hình thời hạn lưu, tự xóa quá hạn và truy vấn `RecordingLog` | Danh sách file, xem lại/xóa, đặt số ngày lưu (mặc định 30), xem nhật ký | FND-02, SC_001, SC_004, SC_009; Admin được xem tất cả, không điều khiển lớp | `feature/sc-013-admin-recordings` |

## 4. Thứ tự triển khai và tích hợp

1. Hoàn thành FND-01; sau đó FND-02, FND-03, FND-04 và FND-05 có thể phát triển song song theo các phụ thuộc ghi trong bảng. Võ Minh Hiếu điều phối tích hợp từ FND-06.
2. Tích hợp xác thực SC_001/SC_002, rồi Dashboard SC_003/SC_004 và Quản lý tài khoản SC_005.
3. Sau khi SC_003 và FND-03 ổn định, triển khai song song Khóa/Mở khóa, Trình chiếu, Phát tài liệu và Thu bài.
4. Hoàn thành Live-view SC_008 trước Ghi hình SC_009; sau đó mới nối Kho ghi hình của Giảng viên và Admin.
5. Mỗi nhánh cần có test phù hợp và được tích hợp qua pull request; Âu Thị Huyền Trang theo dõi kiểm thử tích hợp, Võ Minh Hiếu điều phối review/merge. Không gộp trực tiếp các thay đổi schema/protocol chưa được các module phụ thuộc xác nhận.

## 5. Lưu ý cần chốt khi triển khai

- ERD hiện có 9 thực thể: `User`, `Machine`, `SystemSetting`, `StudentSession`, `SubmissionSession`, `RecordingFile`, `RecordingLog`, `Submission`, `SubmissionFile`. Migration cần đối chiếu lại tên, kiểu dữ liệu, nullability và FK trước khi code DAO.
- Kế hoạch hiện tại nói xóa mềm tài khoản, nhưng ERD chưa có trường `deleted_at`/trạng thái xóa. Thành viên phụ trách SC_005 cần thống nhất với nhóm trước khi chốt migration.
- Đặc tả yêu cầu ghi trạng thái và thời điểm nộp; cần bảo đảm `SubmissionSession`, `Submission` và `SubmissionFile` phân biệt được phiên thu với từng file và từng sinh viên.
- Dữ liệu ghi hình lưu trên ổ đĩa Server; PostgreSQL chỉ lưu metadata, quyền sở hữu, thời hạn và log. Xóa/retention cần giữ nhất quán giữa file trên đĩa và bản ghi DB.
- Không commit mật khẩu, secret hoặc thông tin kết nối thật. Không ghi lại phím bấm, âm thanh hay dữ liệu ngoài phạm vi yêu cầu.
- Các branch trong bảng là tên đề xuất; nếu nhóm dùng mã issue thì thêm mã issue vào tên branch theo quy ước Git đã thống nhất.
