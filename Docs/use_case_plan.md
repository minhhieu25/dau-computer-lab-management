# TÀI LIỆU SƠ ĐỒ USE CASE (CHUẨN UML)
**Dự án:** Hệ thống Quản lý Phòng máy (Client-Server PC)

*Quy tắc áp dụng:* 
- Chỉ có 2 Actor ngoại vi (Con người: Giảng viên, Sinh viên). Không định nghĩa Hệ thống làm Actor.
- Quan hệ Actor - Use Case là Association (Đường thẳng liền, không mũi tên).
- Quan hệ `<<include>>` trỏ từ UC gốc sang UC thành phần. Quan hệ `<<extend>>` trỏ từ UC mở rộng về UC gốc.
- Không vẽ luồng dữ liệu, giao thức (TCP/UDP) hay các tính năng ngầm của hệ thống (nhịp tim, chunking file, ghép file) vào sơ đồ này.

---

## 1. SƠ ĐỒ USE CASE MỨC TỔNG QUÁT

```mermaid
flowchart LR
    %% Định nghĩa Actors
    GV(["🧑‍🏫 Giảng viên"])
    SV(["👨‍🎓 Sinh viên"])
    
    %% System Boundary
    subgraph "Hệ thống Quản lý Phòng máy"
        direction TB
        UC1("Kết nối vào lớp học")
        UC2("Giám sát trạng thái phòng máy")
        UC3("Điều khiển khóa/mở khóa")
        UC4("Trình chiếu & Giám sát màn hình")
        UC5("Phát tài liệu & Thu bài")
    end

    %% Association (Đường thẳng không mũi tên)
    GV --- UC2
    GV --- UC3
    GV --- UC4
    GV --- UC5

    SV --- UC1
    SV --- UC3
    SV --- UC4
    SV --- UC5

    classDef actor fill:#f3f4f6,stroke:#374151,stroke-width:2px,color:#111827
    classDef usecase fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e,rx:20px,ry:20px
    class GV,SV actor
    class UC1,UC2,UC3,UC4,UC5 usecase
```

---

## 2. CÁC SƠ ĐỒ USE CASE PHÂN RÃ

### Sơ đồ 1: Kết nối & Giám sát trạng thái

```mermaid
flowchart LR
    GV(["🧑‍🏫 Giảng viên"])
    SV(["👨‍🎓 Sinh viên"])
    
    subgraph "Kết nối & Giám sát"
        direction TB
        UC_SV_Join("Kết nối vào lớp học")
        UC_SV_Reconn("Tự kết nối lại khi mất mạng")
        
        UC_GV_View("Xem sơ đồ phòng máy")
        UC_GV_Metrics("Xem chỉ số mạng (Ping, mất gói)")
    end

    SV --- UC_SV_Join
    UC_SV_Reconn -.->|"<< extend >>"| UC_SV_Join
    
    GV --- UC_GV_View
    UC_GV_Metrics -.->|"<< extend >>"| UC_GV_View

    classDef actor fill:#f3f4f6,stroke:#374151,stroke-width:2px,color:#111827
    classDef usecase fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e,rx:20px,ry:20px
    class GV,SV actor
    class UC_SV_Join,UC_SV_Reconn,UC_GV_View,UC_GV_Metrics usecase
```

---

### Sơ đồ 2: Khóa/Mở khóa màn hình

```mermaid
flowchart LR
    GV(["🧑‍🏫 Giảng viên"])
    SV(["👨‍🎓 Sinh viên"])
    
    subgraph "Khóa/Mở khóa"
        direction TB
        UC_Lock("Khóa màn hình")
        UC_Unlock("Mở khóa màn hình")
        UC_LockProg("Xem tiến độ khóa")
        
        UC_LateSync("Đồng bộ khóa máy vào muộn")
        UC_Failsafe("Tự nhả khóa khi mất kết nối")
    end

    GV --- UC_Lock
    GV --- UC_Unlock
    GV --- UC_LockProg
    
    SV --- UC_Lock
    SV --- UC_Unlock

    UC_LateSync -.->|"<< extend >>"| UC_Lock
    UC_Failsafe -.->|"<< extend >>"| UC_Lock

    classDef actor fill:#f3f4f6,stroke:#374151,stroke-width:2px,color:#111827
    classDef usecase fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e,rx:20px,ry:20px
    class GV,SV actor
    class UC_Lock,UC_Unlock,UC_LockProg,UC_LateSync,UC_Failsafe usecase
```

---

### Sơ đồ 3: Trình chiếu & Giám sát màn hình

```mermaid
flowchart LR
    GV(["🧑‍🏫 Giảng viên"])
    SV(["👨‍🎓 Sinh viên"])
    
    subgraph "Trình chiếu & Giám sát"
        direction TB
        UC_Proj("Trình chiếu bài giảng")
        UC_Mon("Giám sát màn hình sinh viên")
        
        UC_Record("Ghi hình màn hình sinh viên")
        UC_Metrics("Xem thông số luồng hình")
    end

    GV --- UC_Proj
    GV --- UC_Mon
    
    SV --- UC_Proj
    SV --- UC_Mon

    UC_Record -.->|"<< extend >>"| UC_Mon
    UC_Metrics -.->|"<< extend >>"| UC_Mon
    UC_Metrics -.->|"<< extend >>"| UC_Proj

    classDef actor fill:#f3f4f6,stroke:#374151,stroke-width:2px,color:#111827
    classDef usecase fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e,rx:20px,ry:20px
    class GV,SV actor
    class UC_Proj,UC_Mon,UC_Record,UC_Metrics usecase
```

---

### Sơ đồ 4: Phát tài liệu & Thu bài

```mermaid
flowchart LR
    GV(["🧑‍🏫 Giảng viên"])
    SV(["👨‍🎓 Sinh viên"])
    
    subgraph "Phát tài liệu & Thu bài"
        direction TB
        UC_Push("Phát tài liệu")
        UC_PushMetrics("Xem tiến độ & tốc độ truyền")
        
        UC_Prep("Chuẩn bị bài nộp")
        UC_Pull("Thu bài")
        UC_PullMetrics("Xem tiến độ & tốc độ nộp")
        UC_PullStats("Xem thống kê Đã nộp/Chưa nộp")
        UC_Save("Lưu bài nộp (Gắn IP)")
    end

    GV --- UC_Push
    GV --- UC_Pull
    
    SV --- UC_Push
    SV --- UC_Prep
    SV --- UC_Pull

    UC_PushMetrics -.->|"<< extend >>"| UC_Push
    
    UC_PullMetrics -.->|"<< extend >>"| UC_Pull
    UC_PullStats -.->|"<< extend >>"| UC_Pull
    
    UC_Pull -.->|"<< include >>"| UC_Save

    classDef actor fill:#f3f4f6,stroke:#374151,stroke-width:2px,color:#111827
    classDef usecase fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e,rx:20px,ry:20px
    class GV,SV actor
    class UC_Push,UC_PushMetrics,UC_Prep,UC_Pull,UC_PullMetrics,UC_PullStats,UC_Save usecase
```
