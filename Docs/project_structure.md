# CẤU TRÚC THƯ MỤC PROJECT — JAVA MVC

## 1. Mục đích

Tài liệu này mô tả cây thư mục cho Hệ thống Quản lý & Giám sát Phòng máy theo Java 21, JavaFX 21, Gradle multi-module và PostgreSQL. Gradle scaffold dùng module trực tiếp tại project root; FND-02 bổ sung cấu hình database, mapping Java và Flyway migrations trong module `server`.

Ứng dụng Server và Client dùng MVC. `shared` là thư viện dùng chung, không phải một ứng dụng MVC.

## 2. Cây thư mục đề xuất

```text
project-root/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── shared/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/dau/labmanagement/shared/
│       │   ├── protocol/
│       │   │   ├── message/
│       │   │   └── codec/
│       │   ├── dto/
│       │   └── constant/
│       └── test/java/com/dau/labmanagement/shared/
├── server/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/dau/labmanagement/server/
│       │   ├── ServerApplication.java
│       │   ├── config/
│       │   ├── controller/
│       │   ├── model/
│       │   │   ├── entity/
│       │   │   └── state/
│       │   ├── service/
│       │   ├── dao/
│       │   ├── network/
│       │   │   ├── tcp/
│       │   │   └── udp/
│       │   └── view/
│       ├── main/resources/
│       │   ├── fxml/
│       │   ├── css/
│       │   ├── images/
│       │   ├── config/
│       │   └── db/migration/
│       ├── test/java/com/dau/labmanagement/server/
│       └── test/resources/
├── client/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/dau/labmanagement/client/
│       │   ├── ClientApplication.java
│       │   ├── config/
│       │   ├── controller/
│       │   ├── model/
│       │   │   ├── entity/
│       │   │   └── state/
│       │   ├── service/
│       │   ├── agent/
│       │   └── view/
│       ├── main/resources/
│       │   ├── fxml/
│       │   ├── css/
│       │   ├── images/
│       │   └── config/
│       ├── test/java/com/dau/labmanagement/client/
│       └── test/resources/
├── shared-test/
│   ├── build.gradle.kts
│   └── src/testFixtures/java/com/dau/labmanagement/test/
│       ├── FakeClient.java
│       └── FakeServer.java
├── Docs/
│   ├── project_structure.md
│   ├── ke_hoach_trien_khai.md
│   ├── protocol-spec.md
│   ├── nfr.md
│   ├── test-plan.md
│   └── adr/
└── .gitignore
```

## 3. Trách nhiệm các thành phần MVC

| Thành phần | Thư mục | Trách nhiệm |
|---|---|---|
| Model | `model/` | Đối tượng miền, dữ liệu và trạng thái cần hiển thị; không phụ thuộc trực tiếp vào FXML |
| View | `view/`, `src/main/resources/fxml/`, `css/`, `images/` | Giao diện JavaFX và tài nguyên trình bày; không chứa truy vấn DB hoặc nghiệp vụ |
| Controller | `controller/` | Nhận thao tác từ View, gọi Service, cập nhật Model/hiển thị kết quả; không chứa SQL hoặc xử lý nghiệp vụ dài |
| Service | `service/` | Xử lý nghiệp vụ và điều phối các thao tác; Server gọi DAO, Client điều phối agent/giao tiếp |
| DAO | `dao/` (Server) | Truy cập PostgreSQL qua JDBI; chỉ lớp này phụ trách câu truy vấn và ánh xạ dữ liệu |
| Network / Agent | `network/` (Server), `agent/` (Client) | Xử lý TCP/UDP phía Server và các tác vụ nền phía Client; dùng message/DTO từ `shared` |
| Shared | `shared/` | Protocol, message type, DTO và codec dùng chung; không khai báo trùng ở Server/Client |

Luồng xử lý giao diện:

```text
View (FXML) → Controller → Service → DAO (Server) → PostgreSQL
                   │
                   └── Model / trạng thái giao diện
```

Ở Client, Service làm việc với agent và protocol trong `shared`, không truy cập PostgreSQL trực tiếp:

```text
View (FXML) → Controller → Service → Agent / Network → Server
                   │
                   └── Model / trạng thái giao diện
```

## 4. Quy ước đặt mã nguồn và tài nguyên

- Mỗi ứng dụng có package gốc riêng: `com.dau.labmanagement.server` và `com.dau.labmanagement.client`.
- Protocol, enum mã lệnh và DTO truyền qua mạng chỉ được định nghĩa một lần trong `shared`.
- FXML đặt trong `src/main/resources/fxml/`; controller Java đặt trong `controller/`. Controller được tham chiếu từ FXML theo package/class tương ứng.
- Flyway migration đặt trong `server/src/main/resources/db/migration/` để có trên classpath của ứng dụng Server. Tên file theo định dạng `V1__schema.sql`, `V2__seed.sql`.
- Kiểm thử đơn vị đặt trong `src/test/java/`; tài nguyên test đặt trong `src/test/resources/`. Fixture dùng chung cho integration test đặt trong `shared-test`.
- Module `shared-test` bật Gradle `java-test-fixtures` plugin để dùng source set `src/testFixtures`.
- Cấu hình môi trường thật (mật khẩu, secret, địa chỉ triển khai riêng) không commit vào Git; chỉ cung cấp cấu hình mẫu không chứa bí mật.
- Không tạo package hoặc lớp chỉ để lấp đầy cây thư mục; chỉ thêm khi có chức năng thực tế cần đến.

## 5. Trình tự dựng cấu trúc

1. Tạo Gradle Wrapper, version catalog và các module `shared`, `server`, `client`, `shared-test`.
2. Bổ sung package gốc, entry point JavaFX và thư mục resources cần thiết cho từng ứng dụng.
3. Đưa contract mạng vào `shared`; xác nhận Server và Client cùng phụ thuộc vào một phiên bản contract.
4. Tạo Flyway migration trong resources của Server và kiểm tra migration với PostgreSQL test.
5. Triển khai từng chức năng theo luồng MVC; bổ sung test cho Controller/Service/DAO phù hợp.
