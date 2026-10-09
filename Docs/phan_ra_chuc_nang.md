TÀI LIỆU PHÂN RÃ CHỨC NĂNG HỆ THỐNG QUẢN LÝ PHÒNG MÁY (CLIENT-SERVER)

* BẢNG PHÂN LOẠI THEO LOẠI DỮ LIỆU MẠNG (TCP/UDP):
- Giao thức UDP (Ưu tiên tốc độ, chấp nhận mất gói): Broadcast tìm IP, Nhịp tim và ACK, Luồng hình ảnh trình chiếu, Luồng hình ảnh Live-view giám sát.
- Giao thức TCP (Tin cậy tuyệt đối 100%): Các lệnh điều khiển (Khóa/Mở khóa, Bắt đầu/Dừng trình chiếu, Bắt đầu/Dừng Live-view & Ghi hình), Push file, Pull file.

---
PHÂN HỆ 01: QUẢN LÝ KẾT NỐI VÀ HIỂN THỊ TRẠNG THÁI (UDP)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Tự động tìm IP Server: Phát Broadcast UDP trong mạng LAN để tìm IP Giảng viên.
- 1.2 Báo danh tự động: Sau khi có IP, gửi gói báo danh kết nối.
- 1.3 Phát tín hiệu "nhịp tim": Gửi gói dữ liệu nhỏ mỗi 3 giây để báo hoạt động, đồng thời đính kèm giá trị RTT đo được từ đợt trước lên Server.
- 1.4 Lắng nghe phản hồi (ACK) & Đo RTT: Nhận gói ACK từ Server. Tính toán thời gian vòng đi-về (RTT - ms) từ lúc gửi nhịp tim đến lúc nhận ACK.
- 1.5 Cơ chế tự giải cứu (Fail-safe): Nếu quá 10 giây không nhận được ACK hoặc mất 3 gói ACK liên tiếp, Client tự động: Nhả khóa màn hình, Thoát Focus Mode, Dừng gửi luồng Live-view và Tắt Icon cảnh báo.
- 1.6 Tự kết nối lại: Liên tục thử kết nối lại nếu rớt mạng.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Lắng nghe Broadcast & Báo danh: Phản hồi Broadcast UDP và mở kết nối.
- 2.2 Phản hồi nhịp tim (ACK): Trả lời ngay lập tức khi nhận nhịp tim.
- 2.3 Phân loại trạng thái: Đánh dấu Offline nếu quá 10 giây hoặc mất 3 gói nhịp tim liên tiếp.
- 2.4 Giao diện sơ đồ lưới: Đổi màu ô vuông trực quan (Xanh/Đỏ) theo thời gian thực.
- 2.5 Thu thập RTT & Tỷ lệ mất gói: Đọc giá trị RTT do Client gửi lên. Đếm số gói nhịp tim bị mất để tính tỷ lệ mất gói (%).

* Input 1: Client khởi động và phát Broadcast UDP.
* Input 2: Chuỗi "nhịp tim" gửi liên tục từ Client.
* Output 1: Sơ đồ phòng máy đổi màu Xanh/Đỏ trên Server.
* Output 2: Chỉ số RTT (ms) và Tỷ lệ mất gói (%) hiển thị trên từng ô vuông máy tính.

---
PHÂN HỆ 02: KHÓA/MỞ KHÓA MÀN HÌNH (TCP)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Thực thi khóa màn hình: Nhận lệnh, hiển thị lớp phủ đen kèm văn bản, vô hiệu hóa chuột/phím.
- 1.2 Thực thi mở khóa: Gỡ lớp phủ, trả quyền sử dụng.
- 1.3 Chống lệnh trùng lặp: Lọc các lệnh khóa bị gửi đúp.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Phát lệnh Khóa/Mở khóa: Chọn một hoặc nhiều máy, nhập thông báo chữ và phát lệnh TCP.
- 2.2 Đồng bộ khóa máy vào muộn: Tự ép lệnh khóa cho máy sinh viên đi trễ vừa mới bật lên.
- 2.3 Hiển thị bảng tiến độ khóa: Đo đếm và báo cáo số lượng máy đã khóa thành công.

* Input 1: Danh sách các máy sinh viên bị chọn để khóa.
* Input 2: Lệnh "Khóa/Mở khóa" kèm nội dung câu thông báo.
* Output 1: Máy sinh viên bị đóng băng, hiện thông báo che kín màn hình.
* Output 2: Bảng tiến độ báo cáo số lượng máy đã khóa thành công.

---
PHÂN HỆ 03: TRÌNH CHIẾU VÀ GIÁM SÁT MÀN HÌNH (Lệnh: TCP; Luồng hình ảnh: UDP)

* Quy tắc xử lý mất gói Frame UDP: Mỗi khung hình được chia thành nhiều gói nhỏ, mỗi gói mang Số thứ tự Frame và Số thứ tự Packet. Bên nhận ghép đủ thì hiển thị. Nếu thiếu gói hoặc gói đến trễ, lập tức vứt bỏ toàn bộ Frame đó (không yêu cầu gửi lại). Đếm số gói mất để tính Tỷ lệ mất gói.

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Nhận luồng trình chiếu: Xem hình ảnh trực tiếp từ máy giảng viên.
- 1.2 Focus mode (Trình chiếu): Chặn phím Alt+Tab và khóa chuột khi trình chiếu.
- 1.3 Hiển thị cảnh báo giám sát: Bật icon "Đang bị giám sát màn hình". Nếu Server có ghi hình, icon đổi thành "Đang bị giám sát và ghi hình".
- 1.4 Truyền luồng giám sát ngầm: Chụp liên tục toàn màn hình, băm nhỏ và truyền Video UDP.
- 1.5 Dừng và Giải phóng: Khi nhận lệnh Dừng (TCP), thoát Focus Mode, tắt icon và dừng truyền Video.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Lệnh Bắt đầu/Dừng trình chiếu: Phát lệnh TCP điều khiển.
- 2.2 Phát luồng hình ảnh: Bóp nén, đóng gói UDP và truyền màn hình giảng viên.
- 2.3 Lệnh Yêu cầu/Dừng giám sát: Phát lệnh TCP yêu cầu Client gửi luồng Video.
- 2.4 Ghi hình màn hình Client: Khi đang Live-view, giảng viên có thể phát lệnh TCP "Bắt đầu/Dừng ghi". Server tự động lưu luồng Video nhận được thành định dạng `[IP]_[ngày_giờ].mp4` vào ổ cứng.
- 2.5 Đo thông số luồng hình ảnh (TCP/UDP Dashboard): Tính toán FPS, Băng thông (Mbps) và Tỷ lệ mất gói (%) cho cả luồng Trình chiếu và Live-view.

* Input 1: Lệnh TCP Bắt đầu/Dừng (Trình chiếu, Live-view).
* Input 2: Luồng dữ liệu hình ảnh (Video) bóp nén UDP.
* Input 3: Lệnh TCP "Bắt đầu ghi / Dừng ghi" từ Giảng viên.
* Output 1: Màn hình sinh viên chiếu bài giảng của giáo viên.
* Output 2: Cửa sổ Live-view trên máy giảng viên với màn hình sinh viên.
* Output 3: Thông số FPS, Băng thông (Mbps), và Tỷ lệ mất gói (%) hiển thị cho từng luồng.
* Output 4: File ghi hình Video (.mp4) được lưu trên ổ cứng Server.

---
PHÂN HỆ 04: PHÁT TÀI LIỆU PUSH (Giao thức TCP)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Nhận phân mảnh (Chunking): Tải khối byte nhỏ và ghép lại thành file gốc.
- 1.2 Ghi vào Sandbox: Lưu bắt buộc vào đúng thư mục "D:\ThuMucDungChung\DeBai".

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Băm nhỏ file: Cắt file (kể cả .zip, Revit, CAD) thành hàng nghìn khối nhỏ TCP.
- 2.2 Tính toán đo lường: Tính % hoàn thành, tốc độ (MB/s) và ETA.

* Input 1: File đề bài được Giảng viên chọn.
* Input 2: Danh sách các máy sinh viên nhận file.
* Output 1: File đề bài an toàn trong `D:\ThuMucDungChung\DeBai` của máy sinh viên.
* Output 2: Màn hình Dashboard hiển thị TCP: Tốc độ (MB/s), % hoàn thành và ETA cho từng máy.

---
PHÂN HỆ 05: THU BÀI TỰ ĐỘNG PULL (Giao thức TCP)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Lắng nghe lệnh Thu bài: Khóa thao tác chỉnh sửa tại thư mục nộp bài.
- 1.2 Băm nhỏ và Đẩy phân mảnh: Băm nhỏ các file trong "D:\ThuMucDungChung\NopBai" và chủ động truyền lên Server qua TCP.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Kích hoạt thu bài: Phát lệnh TCP yêu cầu hút dữ liệu từ danh sách máy Online.
- 2.2 Nhận và Ghép phân mảnh: Nhận luồng Byte TCP và ghép file hoàn chỉnh.
- 2.3 Đổi tên tự động và Lưu trữ: Gắn thêm IP máy sinh viên vào trước tên file và lưu vào ổ cứng Server.
- 2.4 Thống kê & Tính toán đo lường: Hiển thị trạng thái "Đã nộp/Chưa nộp". Đo đạc thông số: % hoàn thành, tốc độ tải (MB/s), và ETA cho từng luồng máy đang nộp.

* Input 1: Lệnh "Bắt đầu thu bài" từ Giảng viên.
* Input 2: File bài làm sinh viên đã lưu vào thư mục `D:\ThuMucDungChung\NopBai`.
* Output 1: Server lưu trữ thành công các bài thi đã đổi tên (gắn IP).
* Output 2: Bảng thống kê "Đã nộp" / "Chưa nộp" hiển thị cho Giảng viên.
* Output 3: Màn hình Dashboard TCP hiển thị Tốc độ (MB/s), % hoàn thành và ETA của các máy đang nộp bài.