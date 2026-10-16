1. Workflow Giám sát sinh viên (92%)
Lỗi chính

❌ Đưa trạng thái UI của sinh viên thành bước workflow

Ví dụ:

Plain Text
Thấy icon: Đang bị giám sát màn hình
Icon biến mất

Đây là:

Plain Text
System Response

không phải:

Plain Text
Business Activity

❌ Decision chưa tự nhiên

Plain Text
Tiếp tục thế nào?

↓

Plain Text
Đã xem
Cần lưu bằng chứng

Decision nên là câu điều kiện hơn.

Ví dụ:

Plain Text
Cần ghi hình không?

sẽ chuẩn hơn.

2. Workflow Ghi hình màn hình sinh viên (95%)
Lỗi chính

❌ Vẫn còn một phần UI Response

Plain Text
Icon đổi thành:
Đang bị giám sát và ghi hình
Plain Text
Icon trở về:
Đang bị giám sát màn hình

Những thứ này nên nằm trong:

Plain Text
Đặc tả giao diện

không phải workflow.

❌ Thiếu bước nghiệp vụ

Bạn nhảy:

Plain Text
Dừng ghi hình
↓
Lưu vào kho

Thực tế nên có:

Plain Text
Dừng ghi hình
↓
Lưu file ghi hình
↓
Cập nhật kho ghi hình

Logic sẽ đầy đủ hơn.

3. Workflow Quản lý file ghi hình (93%)
Lỗi chính

❌ Gộp quá nhiều nghiệp vụ

Trong một workflow có:

Plain Text
Xem file
Xóa file
Xem log
Đặt thời gian lưu

Đều hợp lệ.

Nhưng bắt đầu hơi giống:

Plain Text
Menu chức năng

hơn là:

Plain Text
Workflow

❌ Một số nhánh không có bước xác nhận

Ví dụ:

Plain Text
Xóa file
↓
Quay lại danh sách

Thông thường:

Plain Text
Xóa file
↓
Xác nhận thành công
↓
Danh sách cập nhật

sẽ đẹp hơn.

4. Workflow Quản lý tài khoản giảng viên (95%)
Lỗi chính

❌ Thiếu xác nhận kết quả

Ví dụ:

Plain Text
Thêm giảng viên
↓
Danh sách được cập nhật

Nên là:

Plain Text
Thêm giảng viên
↓
Tạo thành công
↓
Danh sách được cập nhật

❌ Thiếu luồng lỗi

Ví dụ:

Plain Text
Tên đăng nhập đã tồn tại
 

Nhưng lỗi này không bắt buộc trong đồ án.

5. Workflow Trình chiếu bài giảng (96%)
Lỗi chính

❌ Sinh viên bị mô tả hơi chi tiết

Plain Text
Theo dõi bài giảng
Không thao tác máy

"Không thao tác máy" là trạng thái sử dụng.

Có thể bỏ cũng được.

Ngoài ra workflow này gần như không có vấn đề.

6. Workflow Khóa / Mở khóa màn hình (96%)
Lỗi chính

❌ Chưa thể hiện rõ lựa chọn

Plain Text
Mở khóa

đang yêu cầu:

Plain Text
Chọn máy cần mở khóa

nhưng:

Plain Text
Khóa

lại có:

Plain Text
Chọn một số máy hoặc cả lớp

Hai vế chưa đối xứng hoàn toàn.

❌ "Theo dõi số máy đã khóa"

Có thể coi là chức năng phụ.

Không sai nhưng không quá cần thiết.

7. Workflow Phát tài liệu (97%)
Lỗi chính

Rất ít.

Chủ yếu chỉ là:

❌ Chưa có bước

Plain Text
Phát hoàn tất

trước khi quay lại sơ đồ phòng máy.

Đây là một trong những workflow đẹp nhất của bạn.

8. Workflow Đăng nhập (97%)
Lỗi chính

❌ Gộp hai điều kiện vào một diamond

Hiện tại:

Plain Text
Thông tin đăng nhập hợp lệ?

nhưng kết quả có:

Plain Text
Sai tài khoản
Đúng nhưng bị khóa
Đúng và hoạt động

Thực tế nên tách:

Plain Text
Thông tin chính xác?

và

Plain Text
Tài khoản bị khóa?
9. Workflow Thu bài (97%)
Lỗi chính

Gần như không có.

Nếu soi kỹ:

❌

Plain Text
Đã thu xong?
``

thực chất là:

Plain Text
Đã nhận bài từ tất cả máy được chọn?

sẽ chính xác hơn.