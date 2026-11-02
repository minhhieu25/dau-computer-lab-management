# Workflow tổng thể

Mô tả cách hệ thống được sử dụng trong một buổi học thực tế. Ba tác nhân: Admin, Giảng viên, Sinh viên.

```mermaid
flowchart TD
    B(["Bắt đầu buổi học"])

    subgraph AD["Admin"]
        direction TB
        A1["Đăng nhập"]
        A2["Quản lý tài khoản giảng viên"]
        A3["Quản lý file ghi hình"]
        A1 --> A2 --> A3
    end

    subgraph GV["Giảng viên"]
        direction TB
        G1["Đăng nhập"]
        G2["Xem sơ đồ phòng máy"]
        G3["Trình chiếu bài giảng"]
        G4["Phát tài liệu"]
        G5["Theo dõi lớp học"]
        G6["Thu bài"]
        G1 --> G2 --> G3 --> G4 --> G5 --> G6
    end

    subgraph SV["Sinh viên"]
        direction TB
        S1["Xem bài giảng"]
        S2["Nhận đề bài"]
        S3["Làm bài"]
        S4["Nộp bài"]
        S1 --> S2 --> S3 --> S4
    end

    B --> A1
    B --> G1
    G3 -.->|"Bài giảng"| S1
    G4 -.->|"Đề bài"| S2
    G5 -.->|"Khóa máy, giám sát khi cần"| S3
    S4 -.->|"Bài nộp"| G6
    G6 --> E(["Kết thúc buổi học"])
    A3 --> E
```

## Ghi chú

- Trong bước "Theo dõi lớp học", giảng viên có thể khóa máy, mở khóa hoặc giám sát sinh viên. Đây là các thao tác tùy chọn, không tách thành nhánh riêng để sơ đồ tổng thể gọn.
- Admin làm việc độc lập với tiến trình buổi học, không điều khiển lớp.
- File ghi hình do giảng viên tạo ra được Admin quản lý ở nhánh riêng.