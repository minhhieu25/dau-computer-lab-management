1. TÀI LIỆU ĐẶC TẢ YÊU CẦU NGHIỆP VỤ (BRD)
Dự án: Hệ thống Quản lý & Giám sát Phòng máy (Client-Server)
Môi trường: Trường Đại học Kiến trúc Đà Nẵng
Kiến trúc: PC Server (Máy Giảng viên) – PC Client (Máy sinh viên)

1. Tổng quan & Vấn đề hiện tại
Sự thiếu linh hoạt trong quản lý: Giảng viên hiện gặp khó khăn trong việc quan sát tổng thể và điều khiển lớp học. Cần một công cụ tập trung để theo dõi trạng thái, khóa máy để yêu cầu sinh viên chú ý, và quản lý luồng dữ liệu.

Khó khăn truyền tải dữ liệu: Sinh viên Kiến trúc sử dụng file đồ án rất nặng. Các phương thức nộp bài truyền thống (USB, Drive) quá chậm, dễ lỗi và phụ thuộc Internet.

2. Phân tích Tác nhân & Nhu cầu (Actor Analysis)
Giảng viên (Dùng PC Server): Cần một phần mềm máy chủ cài trên máy tính tại bục giảng. Qua phần mềm này, giảng viên có thể xem bản đồ trạng thái lớp học, khóa màn hình để yêu cầu sinh viên chú ý, trình chiếu bài giảng, giám sát màn hình sinh viên, và ra lệnh thu/phát file nặng.

Cán bộ quản lý (Admin, dùng phần mềm PC Server): Chịu trách nhiệm về tài khoản và dữ liệu của hệ thống. Admin cấp và quản lý tài khoản giảng viên/sinh viên, đồng thời quản lý các file ghi hình màn hình sinh viên lưu trên Server (xem lại, xóa, quy định thời hạn lưu). Admin không điều khiển lớp học.

Sinh viên (Dùng PC Client): Cần một môi trường học tập minh bạch. Các quá trình nhận đề bài và nộp đồ án phải diễn ra hoàn toàn tự động thông qua một thư mục quy định sẵn trên máy, giảm thiểu thao tác thủ công. Sinh viên đăng nhập một lần vào phần mềm Client bằng tài khoản do Admin cấp để hệ thống biết máy nào là của sinh viên nào. Việc này không thay thế đăng nhập Windows và không ảnh hưởng các tác vụ tự động.

3. Đặc tả Yêu cầu Nghiệp vụ (Business Requirements - BR)

Nhóm Yêu cầu Điều khiển & Giám sát (Qua phần mềm Server)

BR-01 (Giám sát trạng thái): Trên màn hình Server, giảng viên phải xem được sơ đồ lưới phòng máy với 3 trạng thái cho từng máy:
- Xanh (Ổn định): nhịp tim đều, kết nối tốt.
- Vàng (Không ổn định): mất 1 đến 2 nhịp tim liên tiếp, hoặc chất lượng mạng kém (RTT trên 100 ms hoặc mất gói trên 5%).
- Đỏ (Offline): mất 3 nhịp tim liên tiếp (khoảng 30 giây).
Hệ thống dùng cơ chế "Nhịp tim" (Heartbeat) với chu kỳ 10 giây. Việc kết nối được thực hiện tự động: Server định kỳ phát thông báo trong mạng LAN, Client nhận thông báo để biết địa chỉ Server rồi chủ động kết nối tới Server, không cần nhập tay.
Quy tắc mạng: TCP dùng cho dữ liệu cần độ chính xác tuyệt đối (kết nối, lệnh điều khiển, file); UDP Unicast dùng cho nhịp tim và luồng giám sát màn hình; UDP Multicast dùng cho luồng trình chiếu.
- Mỗi ô trên sơ đồ lưới hiển thị sinh viên đang đăng nhập trên máy đó (hoặc "Chưa đăng nhập").

BR-02 (Khóa/Mở khóa không gian học): Giảng viên có thể khóa toàn bộ hoặc chỉ định một số máy sinh viên. Khi bị khóa, máy sinh viên hiện lớp phủ đen kèm thông báo chữ, bị vô hiệu hóa chuột/phím. Hệ thống cần có cơ chế chống lệnh trùng lặp, đồng bộ trạng thái khóa cho máy kết nối muộn, và hiển thị bảng tiến độ khóa trên Server.

BR-03 (Trình chiếu & Giám sát màn hình):
- Trình chiếu: Phát hình ảnh từ màn hình giảng viên tới toàn bộ máy sinh viên bằng một luồng chung (Multicast). Giảng viên bắt đầu và dừng trình chiếu trên Server. Trong lúc trình chiếu, sinh viên ở Focus Mode (khóa Alt+Tab, vô hiệu hóa chuột) để tập trung xem.
- Giám sát màn hình (Live-view): Giảng viên chọn xem trực tiếp màn hình của một sinh viên bất kỳ. Hình ảnh chỉ truyền từ máy sinh viên đó về Server (Unicast), không máy sinh viên nào khác nhận được. Chất lượng hình ảnh là 480p (854x480), 5 FPS. Giảng viên có thể bấm ghi hình lại luồng Live-view này; file ghi hình (.mp4, 480p) được lưu trữ hoàn toàn trên PC Server. Giảng viên có thể dừng giám sát và dừng ghi hình bất kỳ lúc nào.

Nhóm Yêu cầu Quản lý Tài liệu Đồ án (Luồng Server <-> Client)

BR-04 (Phát tài liệu - Push): Giảng viên chọn một file (kể cả file .zip, CAD, Revit) trên PC Server và chọn danh sách máy nhận. File được gửi trực tiếp qua TCP, không cắt ghép, và lưu cố định vào thư mục "D:\ThuMucDungChung\<MSSV>\DeBai" trên máy sinh viên.

BR-05 (Thu bài tự động - Pull): Đến giờ nộp bài, giảng viên ra lệnh trên PC Server. Phần mềm PC Client tự động khóa thao tác chỉnh sửa thư mục nộp bài, rồi gửi các file trong "D:\ThuMucDungChung\<MSSV>\NopBai" về Server qua TCP, không cắt ghép. PC Server tự động nhận và lưu vào thư mục riêng của từng máy theo cấu trúc ThuBai\<user giảng viên>\<ngày_giờ thu>\<IP>_<MSSV>\, giữ nguyên tên file. Thống kê "Đã nộp/Chưa nộp" theo sinh viên.

Nhóm Yêu cầu An toàn, Đo lường & Ràng buộc

BR-06 (An toàn điểm cuối - Fail-safe): Máy sinh viên không bị khóa vĩnh viễn nếu kết nối mạng bị rớt hoặc PC Server sập nguồn. Khi Client mất tín hiệu xác nhận (ACK) từ Server liên tiếp 3 lần (khoảng 30 giây) hoặc kết nối TCP bị đứt, phần mềm Client tự động giải cứu (nhả khóa màn hình, thoát Focus Mode, dừng Live-view, và tắt cảnh báo). Sau đó Client tự động thử kết nối lại khi có mạng.

BR-07 (Chính sách Quyền riêng tư): Việc giám sát màn hình và ghi hình chỉ diễn ra khi Giảng viên chủ động yêu cầu. Xuyên suốt quá trình này, Client luôn hiển thị một Icon cảnh báo trên màn hình sinh viên ("Đang bị giám sát màn hình", đổi thành "Đang bị giám sát và ghi hình" khi có ghi hình). Dữ liệu video chỉ lưu tại Server và chỉ người có quyền (Admin, và Giảng viên đã thực hiện ghi hình đó) mới truy cập được, đồng thời tự động xóa khi quá thời hạn lưu do Admin quy định. Hệ thống hoàn toàn không ghi lại phím bấm, không ghi âm thanh và không quét thư mục ngoài học tập. Thông tin định danh sinh viên chỉ dùng để nhận diện máy trong lớp học, không dùng để theo dõi ngoài giờ học.

BR-08 (Đo lường truyền tải): Phần mềm Server cung cấp một Dashboard đo lường mạng:
- Đối với TCP (Push/Pull): Hiển thị tốc độ (Mbps), % hoàn thành và thời gian dự kiến (ETA) cho từng máy.
- Đối với UDP (Nhịp tim, Trình chiếu, Live-view): Hiển thị RTT (Ping, ms) và Tỷ lệ mất gói (%) cho từng máy; FPS và Băng thông (Mbps) cho từng luồng hình ảnh.
Quy ước đơn vị: tốc độ truyền và băng thông tính bằng Mbps (megabit/giây); dung lượng file tính bằng MB/GB.

Nhóm Yêu cầu Quản trị (Dành cho Cán bộ quản lý)

BR-09 (Quản lý tài khoản & phân quyền):
- Phần mềm Server yêu cầu đăng nhập. Có ba vai trò: Admin,  Giảng viên, Sinh viên. Chỉ tài khoản Giảng viên mới được điều khiển lớp học (khóa máy, trình chiếu, giám sát, ghi hình, phát/thu tài liệu).
- Admin xem danh sách, tạo, khóa/mở khóa, đổi mật khẩu và xóa tài khoản
- Sinh viên đăng nhập trên Client chỉ để định danh; máy chưa đăng nhập vẫn hoạt động bình thường

BR-10 (Quản lý file ghi hình):
- Admin xem danh sách, xem lại và xóa các file ghi hình; Giảng viên xem lại được file do chính mình ghi.
- Admin quy định thời hạn lưu file ghi hình (mặc định 30 ngày, quá hạn tự xóa).
- Hệ thống ghi nhật ký các thao tác nhạy cảm: ai bắt đầu ghi hình, ai xem lại, ai xóa file ghi hình, vào thời điểm nào. Admin xem được nhật ký này.