TÀI LIỆU PHÂN RÃ CHỨC NĂNG HỆ THỐNG QUẢN LÝ PHÒNG MÁY (CLIENT-SERVER)

* BẢNG PHÂN LOẠI THEO LOẠI DỮ LIỆU MẠNG:
- UDP Broadcast (tìm Server): Thông báo Server (beacon).
- UDP Unicast (ưu tiên tốc độ, chấp nhận mất gói): Nhịp tim và ACK, Luồng hình ảnh Live-view giám sát (một Client gửi về Server).
- UDP Multicast (một luồng gửi tới cả lớp): Luồng hình ảnh trình chiếu.
- TCP (tin cậy tuyệt đối 100%): Kết nối báo danh, các lệnh điều khiển (Khóa/Mở khóa, Bắt đầu/Dừng trình chiếu, Bắt đầu/Dừng Live-view & Ghi hình), Push file, Pull file, báo cáo tỷ lệ mất gói của Client.

* VAI TRÒ NGƯỜI DÙNG TRÊN PHẦN MỀM SERVER:
- Giảng viên: điều khiển lớp học (Phân hệ 01 đến 05).
- Admin (Cán bộ quản lý): quản trị tài khoản và file ghi hình (Phân hệ 06).
- Phần mềm Server yêu cầu đăng nhập; vai trò quyết định chức năng được dùng.

* QUY ƯỚC ĐƠN VỊ:
- Tốc độ truyền, băng thông: Mbps (megabit trên giây). Quy đổi: 1 MB/s = 8 Mbps.
- Dung lượng file: MB, GB (byte).

* QUY ƯỚC THỜI GIAN VÀ TRẠNG THÁI:
- Chu kỳ nhịp tim: 10 giây.
- Mỗi máy có 3 trạng thái trên sơ đồ lưới:
  + XANH (Ổn định): nhịp tim đều, RTT <= 100 ms, tỷ lệ mất gói <= 5%.
  + VÀNG (Không ổn định): mất 1 đến 2 nhịp tim liên tiếp, hoặc RTT > 100 ms, hoặc tỷ lệ mất gói > 5%.
  + ĐỎ (Offline): mất 3 nhịp tim liên tiếp (khoảng 30 giây).

---
PHÂN HỆ 01: QUẢN LÝ KẾT NỐI VÀ HIỂN THỊ TRẠNG THÁI (Thông báo: UDP Broadcast; Kết nối: TCP; Nhịp tim: UDP Unicast)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Lắng nghe thông báo Server: Khi khởi động, lắng nghe gói thông báo (beacon) của Server trong mạng LAN để lấy IP và cổng của Server.
- 1.2 Kết nối và báo danh tự động: Sau khi có IP, Client chủ động mở kết nối TCP tới Server và gửi thông tin báo danh. Kết nối này được giữ để nhận lệnh điều khiển.
- 1.3 Phát tín hiệu "nhịp tim": Gửi gói dữ liệu nhỏ mỗi 10 giây (UDP) để báo hoạt động, đồng thời đính kèm giá trị RTT đo được từ đợt trước.
- 1.4 Lắng nghe phản hồi (ACK) & Đo RTT: Nhận gói ACK từ Server. Tính thời gian vòng đi-về (RTT - ms) từ lúc gửi nhịp tim đến lúc nhận ACK.
- 1.5 Cơ chế tự giải cứu (Fail-safe): Nếu mất 3 gói ACK liên tiếp (khoảng 30 giây) hoặc kết nối TCP bị đứt, Client tự động: Nhả khóa màn hình, Thoát Focus Mode, Dừng gửi luồng Live-view và Tắt Icon cảnh báo.
- 1.6 Tự kết nối lại: Liên tục thử kết nối lại tới Server nếu rớt mạng.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Lắng nghe kết nối: Mở cổng TCP chờ Client kết nối và báo danh, quản lý danh sách các kết nối đang mở.
- 2.2 Phát thông báo Server (beacon): Định kỳ phát gói UDP Broadcast chứa IP và cổng của Server để Client tìm thấy.
- 2.3 Phản hồi nhịp tim (ACK): Trả lời ngay lập tức khi nhận nhịp tim.
- 2.4 Phân loại trạng thái: Đánh giá từng máy theo 3 trạng thái Xanh / Vàng / Đỏ theo quy ước ở đầu tài liệu.
- 2.5 Giao diện sơ đồ lưới: Đổi màu ô vuông (Xanh/Vàng/Đỏ) theo thời gian thực.
- 2.6 Thu thập RTT & Tỷ lệ mất gói: Đọc giá trị RTT do Client gửi lên. Đếm số gói nhịp tim bị mất để tính tỷ lệ mất gói (%).

* Input 1: Client khởi động, nhận beacon và kết nối TCP tới Server.
* Input 2: Chuỗi "nhịp tim" gửi liên tục từ Client (10 giây/lần).
* Output 1: Sơ đồ phòng máy đổi màu Xanh/Vàng/Đỏ trên Server.
* Output 2: Chỉ số RTT (ms) và Tỷ lệ mất gói (%) hiển thị trên từng ô vuông máy tính.

---
PHÂN HỆ 02: KHÓA/MỞ KHÓA MÀN HÌNH (TCP)

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Thực thi khóa màn hình: Nhận lệnh, hiển thị lớp phủ đen kèm văn bản, vô hiệu hóa chuột/phím.
- 1.2 Thực thi mở khóa: Gỡ lớp phủ, trả quyền sử dụng.
- 1.3 Chống lệnh trùng lặp: Lọc các lệnh khóa bị gửi đúp.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Phát lệnh Khóa/Mở khóa: Chọn một hoặc nhiều máy, nhập thông báo chữ và phát lệnh qua kết nối TCP.
- 2.2 Đồng bộ khóa máy vào muộn: Tự ép lệnh khóa cho máy sinh viên đi trễ vừa mới kết nối.
- 2.3 Hiển thị bảng tiến độ khóa: Đo đếm và báo cáo số lượng máy đã khóa thành công.

* Input 1: Danh sách các máy sinh viên bị chọn để khóa.
* Input 2: Lệnh "Khóa/Mở khóa" kèm nội dung câu thông báo.
* Output 1: Máy sinh viên bị đóng băng, hiện thông báo che kín màn hình.
* Output 2: Bảng tiến độ báo cáo số lượng máy đã khóa thành công.

---
PHÂN HỆ 03: TRÌNH CHIẾU VÀ GIÁM SÁT MÀN HÌNH (Lệnh: TCP; Trình chiếu: UDP Multicast; Live-view: UDP Unicast)

* Quy tắc xử lý mất gói Frame UDP (áp dụng cho cả trình chiếu và Live-view): Mỗi khung hình được chia thành nhiều gói nhỏ, mỗi gói mang Số thứ tự Frame và Số thứ tự Packet. Bên nhận ghép đủ thì hiển thị. Nếu thiếu gói hoặc gói đến trễ, lập tức vứt bỏ toàn bộ Frame đó (không yêu cầu gửi lại). Đếm số gói mất để tính Tỷ lệ mất gói.
* Thông số luồng hình ảnh của Live-view và Ghi hình: độ phân giải 1280x720 (720p), 10 FPS, khoảng 2 Mbps. Client thu nhỏ ảnh về 720p trước khi gửi, không phụ thuộc độ phân giải màn hình thật của sinh viên.
* Ràng buộc mạng của Multicast: Switch trong phòng máy cần bật IGMP snooping để luồng trình chiếu không bị tràn ra các cổng không tham gia nhóm.
* Phân biệt: Luồng trình chiếu dùng Multicast vì mọi máy cùng xem một nội dung. Luồng Live-view dùng Unicast, chỉ gửi từ máy sinh viên được chọn về Server, không máy nào khác nhận được.

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Nhận luồng trình chiếu: Tham gia nhóm Multicast khi nhận lệnh Bắt đầu trình chiếu, hiển thị hình ảnh trực tiếp từ máy giảng viên.
- 1.2 Đo và báo cáo tỷ lệ mất gói trình chiếu: Client tự đếm gói mất của luồng Multicast và báo về Server qua kết nối TCP (Server không tự biết Client nhận tốt hay không).
- 1.3 Focus mode (Trình chiếu): Chặn phím Alt+Tab và khóa chuột khi trình chiếu.
- 1.4 Hiển thị cảnh báo giám sát: Bật icon "Đang bị giám sát màn hình". Nếu Server có ghi hình, icon đổi thành "Đang bị giám sát và ghi hình".
- 1.5 Truyền luồng giám sát ngầm: Chụp liên tục toàn màn hình, thu nhỏ về 720p, chia thành gói nhỏ và truyền Video UDP Unicast về Server.
- 1.6 Dừng và Giải phóng: Khi nhận lệnh Dừng (TCP), rời nhóm Multicast, thoát Focus Mode, tắt icon và dừng truyền Video.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Lệnh Bắt đầu/Dừng trình chiếu: Phát lệnh TCP điều khiển.
- 2.2 Phát luồng hình ảnh trình chiếu: Bóp nén, đóng gói và phát một luồng UDP Multicast chứa màn hình giảng viên tới cả lớp.
- 2.3 Lệnh Yêu cầu/Dừng giám sát: Phát lệnh TCP yêu cầu một Client được chọn gửi luồng Video về Server.
- 2.4 Ghi hình màn hình Client: Khi đang Live-view, giảng viên có thể phát lệnh "Bắt đầu/Dừng ghi". Server lưu luồng Video nhận được (720p, 10 FPS) thành file `[IP]_[ngày_giờ].mp4` vào thư mục ghi hình trên ổ cứng Server, đồng thời ghi nhật ký (tài khoản nào bắt đầu ghi, máy nào, lúc nào).
- 2.5 Đo thông số luồng hình ảnh: Tính FPS, Băng thông (Mbps) và Tỷ lệ mất gói (%). Luồng Live-view do Server tự đo. Luồng trình chiếu lấy tỷ lệ mất gói từ báo cáo của Client.

* Input 1: Lệnh TCP Bắt đầu/Dừng (Trình chiếu, Live-view).
* Input 2: Luồng dữ liệu hình ảnh (Video) bóp nén UDP.
* Input 3: Lệnh "Bắt đầu ghi / Dừng ghi" từ Giảng viên.
* Output 1: Màn hình sinh viên chiếu bài giảng của giáo viên.
* Output 2: Cửa sổ Live-view trên máy giảng viên với màn hình sinh viên.
* Output 3: Thông số FPS, Băng thông (Mbps), và Tỷ lệ mất gói (%) hiển thị cho từng luồng.
* Output 4: File ghi hình Video (.mp4, 720p) được lưu trên ổ cứng Server.

---
PHÂN HỆ 04: PHÁT TÀI LIỆU PUSH (Giao thức TCP)

* Cách truyền: Server gửi cả file qua một kết nối TCP, không tự cắt hay ghép file (TCP tự đảm nhận). Trước dữ liệu, Server gửi một tiêu đề gồm tên file và kích thước để Client biết khi nào nhận đủ và tính được % tiến độ.

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Nhận file: Đọc tiêu đề (tên file, kích thước), nhận dữ liệu qua TCP và ghi thẳng xuống đĩa.
- 1.2 Ghi vào Sandbox: Lưu bắt buộc vào đúng thư mục "D:\ThuMucDungChung\DeBai".

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Gửi file: Gửi tiêu đề rồi gửi dữ liệu file (kể cả .zip, Revit, CAD) qua TCP tới các máy được chọn.
- 2.2 Tính toán đo lường: Tính % hoàn thành, tốc độ (Mbps) và ETA cho từng máy.

* Input 1: File đề bài được Giảng viên chọn.
* Input 2: Danh sách các máy sinh viên nhận file.
* Output 1: File đề bài an toàn trong `D:\ThuMucDungChung\DeBai` của máy sinh viên.
* Output 2: Màn hình Dashboard hiển thị TCP: Tốc độ (Mbps), % hoàn thành và ETA cho từng máy.

---
PHÂN HỆ 05: THU BÀI TỰ ĐỘNG PULL (Giao thức TCP)

* Cách truyền: Giống phân hệ 04. Client gửi từng file qua TCP, mỗi file có tiêu đề (tên file, kích thước) đi trước, không cắt ghép file.

1. Các chức năng con trên PC Client (Máy sinh viên)
- 1.1 Lắng nghe lệnh Thu bài: Khóa thao tác chỉnh sửa tại thư mục nộp bài.
- 1.2 Gửi file bài làm: Chủ động gửi các file trong "D:\ThuMucDungChung\NopBai" lên Server qua TCP.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Kích hoạt thu bài: Phát lệnh TCP yêu cầu thu bài từ danh sách máy Online.
- 2.2 Nhận file: Nhận file qua TCP theo tiêu đề và ghi xuống đĩa.
- 2.3 Đổi tên tự động và Lưu trữ: Gắn thêm IP máy sinh viên vào trước tên file và lưu vào ổ cứng Server.
- 2.4 Thống kê & Tính toán đo lường: Hiển thị trạng thái "Đã nộp/Chưa nộp". Đo thông số: % hoàn thành, tốc độ (Mbps) và ETA cho từng máy đang nộp.

* Input 1: Lệnh "Bắt đầu thu bài" từ Giảng viên.
* Input 2: File bài làm sinh viên đã lưu vào thư mục `D:\ThuMucDungChung\NopBai`.
* Output 1: Server lưu trữ thành công các bài thi đã đổi tên (gắn IP).
* Output 2: Bảng thống kê "Đã nộp" / "Chưa nộp" hiển thị cho Giảng viên.
* Output 3: Màn hình Dashboard TCP hiển thị Tốc độ (Mbps), % hoàn thành và ETA của các máy đang nộp bài.

---
PHÂN HỆ 06: QUẢN TRỊ TÀI KHOẢN VÀ DỮ LIỆU GHI HÌNH (Chạy hoàn toàn trên PC Server, không dùng mạng tới Client)

1. Các chức năng con trên PC Client (Máy sinh viên)
- Không có.

2. Các chức năng con trên PC Server (Máy giảng viên)
- 2.1 Đăng nhập và phân quyền: Yêu cầu tài khoản và mật khẩu khi mở phần mềm Server. Vai trò Giảng viên chỉ dùng được Phân hệ 01 đến 05; vai trò Admin dùng được Phân hệ 06. Mật khẩu được lưu dưới dạng băm, không lưu nguyên văn.
- 2.2 Quản lý tài khoản Giảng viên: Admin tạo mới, khóa/mở khóa, đổi mật khẩu và xóa tài khoản Giảng viên.
- 2.3 Quản lý file ghi hình: Admin xem danh sách file ghi hình (theo IP máy, ngày giờ, tài khoản đã ghi), xem lại và xóa file. Giảng viên chỉ xem lại được file do chính mình ghi.
- 2.4 Thiết lập thời hạn lưu: Admin quy định số ngày lưu file ghi hình (mặc định 30 ngày). Server tự động xóa file quá hạn.
- 2.5 Nhật ký thao tác nhạy cảm: Ghi lại ai bắt đầu ghi hình, ai xem lại, ai xóa file ghi hình và thời điểm. Admin xem được nhật ký này.

* Input 1: Tài khoản và mật khẩu đăng nhập.
* Input 2: Thông tin tài khoản Giảng viên do Admin nhập (tạo, khóa, đổi mật khẩu, xóa).
* Input 3: Lệnh xem lại / xóa file ghi hình và giá trị thời hạn lưu từ Admin.
* Output 1: Phần mềm Server hiển thị đúng chức năng theo vai trò sau khi đăng nhập.
* Output 2: Danh sách tài khoản Giảng viên và trạng thái (đang dùng / bị khóa).
* Output 3: Danh sách file ghi hình, cửa sổ xem lại, và kết quả xóa (thủ công hoặc tự động khi quá hạn).
* Output 4: Bảng nhật ký thao tác ghi hình.