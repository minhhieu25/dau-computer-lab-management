1. TÀI LIỆU ĐẶC TẢ YÊU CẦU NGHIỆP VỤ (BRD)
Dự án: Hệ thống Quản lý & Giám sát Phòng máy (Client-Server)
Môi trường: Trường Đại học Kiến trúc Đà Nẵng
Kiến trúc: PC Server (Máy Giảng viên) – PC Client (Máy sinh viên)

1. Tổng quan & Vấn đề hiện tại
Sự thiếu linh hoạt trong quản lý: Giảng viên hiện gặp khó khăn trong việc quan sát tổng thể và điều khiển lớp học. Cần một công cụ tập trung để theo dõi trạng thái, khóa máy để yêu cầu sinh viên chú ý, và quản lý luồng dữ liệu.

Khó khăn truyền tải dữ liệu: Sinh viên Kiến trúc sử dụng file đồ án rất nặng. Các phương thức nộp bài truyền thống (USB, Drive) quá chậm, dễ lỗi và phụ thuộc Internet.

2. Phân tích Tác nhân & Nhu cầu (Actor Analysis)
Giảng viên (Dùng PC Server): Cần một phần mềm máy chủ cài trên máy tính tại bục giảng. Qua phần mềm này, giảng viên có thể xem bản đồ trạng thái lớp học, khóa màn hình để yêu cầu sinh viên chú ý, trình chiếu bài giảng, và ra lệnh thu/phát file nặng.

Sinh viên (Dùng PC Client): Cần một môi trường học tập minh bạch. Các quá trình nhận đề bài và nộp đồ án phải diễn ra hoàn toàn tự động thông qua một thư mục quy định sẵn trên máy, giảm thiểu thao tác thủ công.

3. Đặc tả Yêu cầu Nghiệp vụ (Business Requirements - BR)

Nhóm Yêu cầu Điều khiển & Giám sát (Qua phần mềm Server)

BR-01 (Giám sát trạng thái): Trên màn hình Server, giảng viên phải xem được sơ đồ lưới phòng máy. Hệ thống sử dụng cơ chế "Nhịp tim" (Heartbeat - mỗi 3 giây) để cập nhật trạng thái Online/Offline (đánh dấu Offline sau 10 giây hoặc mất 3 gói tin liên tiếp). Client có khả năng tự báo danh và tự tìm IP Server. *Lưu ý quy tắc mạng:* Hệ thống sử dụng TCP cho dữ liệu cần độ chính xác tuyệt đối (lệnh, file) và UDP cho dữ liệu cần tính thời gian thực (nhịp tim, hình ảnh).

BR-02 (Khóa/Mở khóa không gian học): Giảng viên có thể khóa toàn bộ hoặc chỉ định một số máy sinh viên. Khi bị khóa, máy sinh viên hiện lớp phủ đen kèm thông báo chữ, bị vô hiệu hóa chuột/phím. Hệ thống cần có cơ chế chống lệnh trùng lặp, đồng bộ trạng thái khóa cho máy bật muộn, và hiển thị bảng tiến độ khóa trên Server.

BR-03 (Trình chiếu & Giám sát màn hình): 
- Trình chiếu: Phát hình ảnh từ màn hình giảng viên xuống toàn bộ máy sinh viên. Sinh viên bị đưa vào Focus Mode (khóa Alt+Tab, vô hiệu hóa chuột) để tập trung xem.
- Giám sát màn hình (Live-view): Giảng viên chọn xem trực tiếp màn hình của một sinh viên bất kỳ. Giảng viên có thể bấm ghi hình lại luồng Live-view này, file ghi hình (.mp4) được lưu trữ hoàn toàn trên PC Server.

Nhóm Yêu cầu Quản lý Tài liệu Đồ án (Luồng Server <-> Client)

BR-04 (Phát tài liệu - Push): Giảng viên chọn một file (kể cả file .zip, CAD, Revit) trên PC Server và chọn danh sách máy nhận. Phần mềm tự động băm nhỏ (chunking), đẩy xuống lưu cố định vào thư mục "D:\ThuMucDungChung\DeBai" trên máy sinh viên.

BR-05 (Thu bài tự động - Pull): Đến giờ nộp bài, giảng viên ra lệnh trên PC Server. Phần mềm PC Client tự động khóa thư mục nộp bài, băm nhỏ file bài làm trong "D:\ThuMucDungChung\NopBai" và đẩy ngược về Server. PC Server tự động nhận, ghép mảnh, lưu vào ổ cứng với tên mới có gắn mác IP sinh viên. Báo cáo thống kê "Đã nộp/Chưa nộp" được hiển thị cho Giảng viên.

Nhóm Yêu cầu An toàn, Đo lường & Ràng buộc

BR-06 (An toàn điểm cuối - Fail-safe): Máy sinh viên không bị khóa vĩnh viễn nếu kết nối mạng bị rớt hoặc PC Server sập nguồn. Dựa vào việc mất tín hiệu xác nhận (ACK) từ Server quá 10 giây hoặc rớt 3 gói liên tiếp, phần mềm Client tự động giải cứu (nhả khóa màn hình, thoát Focus Mode, dừng Live-view, và tắt cảnh báo).

BR-07 (Chính sách Quyền riêng tư): Việc giám sát màn hình và ghi hình chỉ diễn ra khi Giảng viên chủ động yêu cầu. Xuyên suốt quá trình này, Client luôn hiển thị một Icon cảnh báo trên màn hình sinh viên. Dữ liệu video chỉ lưu tại Server, hệ thống hoàn toàn không ghi lại phím bấm, không ghi âm thanh và không quét thư mục ngoài học tập.

BR-08 (Đo lường truyền tải): Phần mềm Server cung cấp một Dashboard đo lường mạng chi tiết:
- Đối với TCP (Push/Pull): Hiển thị tốc độ (MB/s), % hoàn thành và thời gian dự kiến (ETA) cho từng máy.
- Đối với UDP (Nhịp tim/Video): Hiển thị RTT (Ping), Tỷ lệ mất gói (Packet loss %), FPS và Băng thông (Mbps) cho từng luồng kết nối.