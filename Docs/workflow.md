# WORKFLOW HỆ THỐNG QUẢN LÝ PHÒNG MÁY

**Dự án:** Hệ thống Quản lý & Giám sát Phòng máy (Client-Server)
**Tác nhân:** Admin, Giảng viên, Sinh viên

---

## 0. Workflow tổng thể

```mermaid
flowchart TD
    START(["Trước buổi học"]) --> A1

    subgraph ADM["Admin"]
        direction TB
        A1["Quản lý tài khoản giảng viên"]
        A2["Quản lý file ghi hình"]
        A3["Xem nhật ký hệ thống"]
    end

    subgraph GV["Giảng viên"]
        direction TB
        G1["Đăng nhập"] --> G2["Xem sơ đồ phòng máy"]
        G2 --> G3["Trình chiếu bài giảng"]
        G3 --> G4["Phát tài liệu"]
        G4 --> G6["Theo dõi lớp làm bài"]
        G6 --> G7{"Cần can thiệp?"}
        G7 -->|"Máy mất tập trung"| G8["Khóa/Mở khóa màn hình"]
        G8 --> G6
        G7 -->|"Kiểm tra bài làm của một sinh viên"| G9["Giám sát sinh viên"]
        G9 --> G10{"Cần lưu bằng chứng?"}
        G10 -->|"Có"| G11["Ghi hình màn hình sinh viên"]
        G11 --> G6
        G10 -->|"Không"| G6
        G7 -->|"Hết giờ làm bài"| G13["Thu bài"]
        G13 --> G14["Xem thống kê nộp bài"]
        G14 --> END_G(["Kết thúc buổi học"])
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Xem bài giảng"]
        S2["Nhận tài liệu"]
        S3["Làm bài"]
        S4["Nộp bài"]
        S2 --> S3 --> S4
    end

    A1 -.->|"Tài khoản để đăng nhập"| G1
    G3 -.->|"Bài giảng được chiếu"| S1
    G4 -.->|"Đề bài được gửi tới máy"| S2
    G8 -.->|"Máy bị khóa, tạm dừng làm bài"| S3
    G8 -.->|"Máy được mở khóa, làm tiếp"| S3
    G9 -.->|"Sinh viên đang được giám sát"| S3
    S4 -.->|"Bài nộp được thu"| G13
    G11 -.->|"File ghi hình lưu vào kho"| A2
    END_G -.->|"Sau giờ học"| A2
    A2 --> A3
```

---

## 1. Đăng nhập

```mermaid
flowchart TD
    A(["Mở phần mềm Server"]) --> B["Nhập tên đăng nhập và mật khẩu"]
    B --> C{"Thông tin đăng nhập chính xác?"}
    C -->|"Không"| D["Báo thông tin đăng nhập không đúng"]
    D --> B
    C -->|"Có"| E{"Tài khoản bị khóa?"}
    E -->|"Có"| F["Báo tài khoản bị khóa, liên hệ Admin"]
    F --> X(["Kết thúc"])
    E -->|"Không"| G{"Vai trò của tài khoản?"}
    G -->|"Admin"| H(["Vào trang quản trị"])
    G -->|"Giảng viên"| I(["Vào sơ đồ phòng máy"])
```

---

## 2. Quản lý tài khoản giảng viên

```mermaid
flowchart TD
    A(["Admin mở Quản lý tài khoản giảng viên"]) --> B["Xem danh sách giảng viên và trạng thái tài khoản"]
    B --> C{"Cần thực hiện việc gì?"}
    C -->|"Thêm giảng viên"| D["Nhập tên đăng nhập, họ tên, mật khẩu ban đầu"]
    D --> D1["Tạo tài khoản thành công"]
    C -->|"Khóa tài khoản"| E["Chọn giảng viên và khóa"]
    E --> E1["Khóa tài khoản thành công"]
    C -->|"Mở khóa tài khoản"| F["Chọn giảng viên và mở khóa"]
    F --> F1["Mở khóa tài khoản thành công"]
    C -->|"Đặt lại mật khẩu"| G["Chọn giảng viên, nhập mật khẩu mới"]
    G --> G1["Đặt lại mật khẩu thành công"]
    C -->|"Xóa tài khoản"| H["Chọn giảng viên và xác nhận xóa"]
    H --> H1["Xóa tài khoản thành công"]
    D1 --> I["Danh sách giảng viên được cập nhật"]
    E1 --> I
    F1 --> I
    G1 --> I
    H1 --> I
    I --> B
    C -->|"Không cần làm gì thêm"| Z(["Quay lại menu Admin"])
```

---

## 3. Quản lý file ghi hình

```mermaid
flowchart TD
    subgraph AD["Admin"]
        direction TB
        A1(["Mở kho file ghi hình"]) --> A2["Xem danh sách file: máy, ngày giờ, người ghi"]
        A2 --> A3{"Cần xử lý file nào?"}
        A3 -->|"Xem lại file"| A4["Chọn file và phát lại"]
        A4 --> A2
        A3 -->|"Xóa file"| A5["Chọn file và xác nhận xóa"]
        A5 --> A6["Xóa file thành công"]
        A6 --> A7["Danh sách file được cập nhật"]
        A7 --> A2
        A3 -->|"Đổi thời hạn lưu"| A8["Nhập số ngày lưu, mặc định 30 ngày"]
        A8 --> A9["Lưu thời hạn mới"]
        A9 --> A2
        A3 -->|"Xem nhật ký ghi hình"| A10["Xem ai đã ghi, xem lại, xóa file và lúc nào"]
        A10 --> A2
        A3 -->|"Không cần làm gì thêm"| A11(["Quay lại menu Admin"])
    end

    subgraph GV["Giảng viên"]
        direction TB
        G1(["Mở kho file ghi hình"]) --> G2["Xem danh sách file do mình ghi"]
        G2 --> G3["Chọn file và xem lại"]
        G3 --> G2
        G2 -->|"Không cần xem thêm"| G4(["Thoát kho file ghi hình"])
    end
```

---

## 4. Trình chiếu bài giảng

```mermaid
flowchart TD
    subgraph GV["Giảng viên"]
        direction TB
        G1(["Từ sơ đồ phòng máy, chọn Trình chiếu"]) --> G2["Bắt đầu trình chiếu màn hình giảng viên"]
        G2 --> G3["Trình bày bài giảng"]
        G3 --> G4["Chọn Dừng trình chiếu"]
        G4 --> G5(["Quay lại sơ đồ phòng máy"])
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Theo dõi bài giảng trên máy của mình"]
        S2(["Kết thúc theo dõi khi giảng viên dừng trình chiếu"])
        S1 --> S2
    end

    G2 -.-> S1
    G4 -.-> S2
```

---

## 5. Khóa/Mở khóa màn hình

```mermaid
flowchart TD
    subgraph GV["Giảng viên"]
        direction TB
        G1(["Từ sơ đồ phòng máy"]) --> G2{"Khóa hay mở khóa?"}
        G2 -->|"Khóa"| G3["Chọn máy hoặc cả lớp"]
        G3 --> G4["Nhập thông báo hiển thị cho sinh viên"]
        G4 --> G5["Xác nhận khóa"]
        G5 --> G6["Khóa thành công"]
        G6 --> G9(["Quay lại sơ đồ phòng máy"])
        G2 -->|"Mở khóa"| G7["Chọn máy hoặc cả lớp"]
        G7 --> G8["Xác nhận mở khóa"]
        G8 --> G8A["Mở khóa thành công"]
        G8A --> G9
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Nhận thông báo của giảng viên và tạm dừng làm bài"]
        S2["Làm tiếp bài khi được mở khóa"]
        S1 --> S2
    end

    G5 -.-> S1
    G8 -.-> S2
```

---

## 6. Phát tài liệu

```mermaid
flowchart TD
    subgraph GV["Giảng viên"]
        direction TB
        G1(["Từ sơ đồ phòng máy, chọn Phát tài liệu"]) --> G2["Chọn file đề bài trên máy giảng viên"]
        G2 --> G3["Chọn máy nhận: cả lớp hoặc một số máy"]
        G3 --> G4["Bắt đầu phát"]
        G4 --> G5["Theo dõi tiến độ từng máy"]
        G5 --> G6{"Còn máy chưa nhận đề bài?"}
        G6 -->|"Có"| G5
        G6 -->|"Không"| G7["Phát hoàn tất"]
        G7 --> G8(["Quay lại sơ đồ phòng máy"])
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Nhận đề bài từ giảng viên"]
        S2["Mở và đọc đề bài"]
        S3(["Bắt đầu làm bài"])
        S1 --> S2 --> S3
    end

    G4 -.-> S1
```

---

## 7. Giám sát sinh viên

```mermaid
flowchart TD
    subgraph GV["Giảng viên"]
        direction TB
        G1(["Từ sơ đồ phòng máy, chọn 1 máy sinh viên"]) --> G2["Bắt đầu giám sát"]
        G2 --> G3["Xem màn hình sinh viên trực tiếp"]
        G3 --> G4{"Cần ghi hình không?"}
        G4 -->|"Có"| G5(["Thực hiện Ghi hình màn hình sinh viên"])
        G5 --> G6{"Cần tiếp tục giám sát?"}
        G4 -->|"Không"| G6
        G6 -->|"Có"| G3
        G6 -->|"Không"| G7["Dừng giám sát"]
        G7 --> G8(["Quay lại sơ đồ phòng máy"])
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Tiếp tục làm bài trong lúc được giám sát"]
    end

    G2 -.-> S1
```

---

## 8. Ghi hình màn hình sinh viên

```mermaid
flowchart TD
    G1(["Đang giám sát một máy sinh viên"]) --> G2["Bắt đầu ghi hình"]
    G2 --> G3["Tiếp tục theo dõi màn hình sinh viên trong lúc ghi"]
    G3 --> G4["Dừng ghi hình"]
    G4 --> G5["Lưu file ghi hình"]
    G5 --> G6["Cập nhật kho ghi hình"]
    G6 --> G7(["Quay lại giám sát"])
```

---

## 9. Thu bài

```mermaid
flowchart TD
    subgraph GV["Giảng viên"]
        direction TB
        G1(["Từ sơ đồ phòng máy, chọn Thu bài"]) --> G2["Chọn máy hoặc cả lớp"]
        G2 --> G3["Bắt đầu thu bài"]
        G3 --> G4["Theo dõi tiến độ thu bài từng máy"]
        G4 --> G5{"Đã nhận bài từ tất cả máy được chọn?"}
        G5 -->|"Chưa"| G4
        G5 -->|"Rồi"| G6["Xem danh sách Đã nộp và Chưa nộp"]
        G6 --> G7(["Quay lại sơ đồ phòng máy"])
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Lưu bài làm vào thư mục NopBai"]
    end

    S1 -.->|"Bài sẵn sàng để thu"| G4
```
