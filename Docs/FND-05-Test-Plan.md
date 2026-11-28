# FND-05 Test Plan

## 1. Muc tieu va pham vi

Tai lieu nay truy vet cac BR-01 den BR-10 trong `Docs/dac_ta_yeu_cau.md` va xac dinh cach kiem thu theo tung tang. Pham vi FND-05 gom cau hinh JUnit 5, test harness loopback trong `shared-test`, xac nhan ket noi PostgreSQL hien co, va chuan bi test DAO sau khi FND-02 ban giao schema/DAO.

Test harness chi dung TCP loopback va dong goi dong chuoi newline-delimited noi bo test; day khong phai protocol san xuat, khong thay the FND-03. Khong tao schema gia. Khong kiem thu ma nghiep vu cua FND-02/FND-03/FND-04 trong pham vi nay.

## 2. Moi truong kiem thu

| Thanh phan | Cau hinh |
|---|---|
| OS | Windows (may phat trien) |
| JDK | Java 21 toolchain qua Gradle |
| Build | Gradle Wrapper 8.14.3, Kotlin DSL |
| Test framework | JUnit Jupiter 5.11.4, JUnit Platform |
| DB test | Zonky embedded-postgres 2.1.0; PostgreSQL JDBC 42.7.7 |
| Network harness | JDK TCP sockets tren loopback, cong ephemeral, timeout huu han |
| Modules | `shared-test`, `server`; DAO tests cho `server` chi bat dau khi co schema/DAO FND-02 |

## 3. Phuong phap va phan loai

- **Unit Test (UT):** kiem tra logic don le/validation/state cua lop ma khong can dich vu ngoai.
- **Integration Test (IT):** ket hop cac thanh phan that trong cung tien trinh; harness loopback xac minh TCP va trao doi fixture.
- **Network Test (NT):** xac minh ket noi, timeout, mat ket noi tren loopback; multicast, UDP va mang LAN that can test lab rieng.
- **Manual Test (MT):** checklist giao dien, thao tac nguoi dung, Windows input, file va danh gia tai phong may.

Test fixture dung timeout cho connect, accept va read; cac test duoc bao ve boi timeout JUnit de tranh treo vo han. Moi test DB trong tuong lai phai tao database/schema co vong doi rieng hoac don sach trong transaction phu hop migration va DAO that.

## 4. Dieu kien bat dau va ket thuc

**Bat dau:** Gradle Wrapper va Java 21 kha dung; dependency co the tai; source compile; FND-03 cung cap contract cho test tich hop protocol; FND-02 cung cap migration va DAO that truoc khi bat dau DAO test.

**Ket thuc:** cac test thuoc pham vi da chay co ket qua Gradle thuc te; khong con FAIL trong pham vi FND-05; cac case chua chay/bi chan duoc ghi ro, khong tu gan PASS; `:shared-test:test`, `:server:test` va `build` duoc bao cao dung ket qua thuc te.

## 5. Quy uoc trang thai

| Trang thai | Y nghia |
|---|---|
| `NOT_RUN` | Chua thuc thi; khong phai PASS. |
| `PASS` / `FAIL` / `SKIPPED` | Chi ghi sau khi co ket qua chay thuc te. |
| `BLOCKED_BY_FND_02` | Cho migration/schema/DAO that cua FND-02. |
| `BLOCKED_BY_FND_03` | Cho protocol/network contract cua FND-03. |
| `BLOCKED_BY_FEATURE` | Cho chuc nang ung dung tuong ung duoc trien khai. |

## 6. Test cases BR-01 den BR-10

Tat ca BR case duoi day chua duoc thuc thi vi chuc nang/dependency chua san sang. Cac case ghi dependency ro rang, khong duoc xem la PASS.

| Test ID | BR ID | Loai | Tien dieu kien | Du lieu dau vao | Buoc thuc hien | Ket qua mong doi | Trang thai |
|---|---|---|---|---|---|---|---|
| BR01-NT-01 | BR-01 | NT, MT | FND-03 heartbeat/discovery; server/client va dashboard san sang | 1 client; heartbeat moi 10 s; RTT 100/101 ms; packet loss 5/5.1%; mat 1, 2, 3 heartbeat | Ket noi client; quan sat tu dong discovery; lan luot tao cac nguong heartbeat/RTT/loss; kiem tra ten sinh vien va may chua dang nhap | Xanh khi on dinh; Vang khi mat 1-2 heartbeat, RTT >100 ms hoac loss >5%; Do sau 3 heartbeat (~30 s); hien thi dung sinh vien hoac "Chua dang nhap" | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR02-IT-01 | BR-02 | IT, MT | FND-03 command contract; Client overlay/input control | LOCK/UNLOCK cho tat ca va tap con; commandId trung lap; client ket noi muon | Gui lock/unlock; gui lai commandId; ket noi client sau khi da lock; theo doi tien do | Dung may bi anh huong; overlay va thong bao hien thi; input bi khoa; command lap khong xu ly hai lan; client moi dong bo state; tien do chinh xac | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR03-MT-01 | BR-03 | NT, MT | FND-03 UDP contract; may phong lab; capture/recording implementation | Luong man hinh; multicast group; live-view 854x480, 5 FPS; MP4 | Bat/tat trinh chieu; kiem tra nhieu client; bat/dung live-view mot client; bat/dung recording | Mot luong multicast toi client; Focus Mode dung; live-view unicast chi ve server; kich thuoc/FPS dat yeu cau; recording MP4 480p luu tren server | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR04-IT-01 | BR-04 | IT, MT | FND-03 TCP file contract; Client da dinh danh | Tep thuong, ZIP/CAD/Revit; nhieu may nhan chon loc | Chon tep tren server; chon client; push; so sanh ten, kich thuoc va noi dung tep | Tep den dung client qua TCP, noi dung/toan ven; luu tai `D:\ThuMucDungChung\<MSSV>\DeBai`; khong gui toi may ngoai danh sach | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR05-IT-01 | BR-05 | IT, MT | FND-02 neu co persistence; FND-03 TCP/file contract; Client folder policy | Tep trong `NopBai`; nhieu MSSV/IP; folder rong | Phat lenh thu; kiem tra folder bi khoa; nhan tep va doi chieu server; kiem tra thong ke | Tep duoc gui qua TCP, giu ten/noi dung va luu dung thu muc theo giang vien/thoi diem/IP/MSSV; bao cao Da nop/Chua nop dung | `BLOCKED_BY_FND_02`, `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR06-NT-01 | BR-06 | NT, MT | FND-03 ACK/timeout contract; Client fail-safe implementation | Mat 3 ACK (~30 s); dong socket; server tat dot ngot | Khoa client; gay tung kieu mat ket noi; theo doi trang thai client va ket noi lai | Client tu mo khoa, thoat Focus Mode, dung live-view, tat canh bao khi mat ACK/TCP; tu thu ket noi lai khi mang phuc hoi | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR07-MT-01 | BR-07 | MT | FND-02 recording metadata/permission neu co; FND-03 live-view; UI Client | Yeu cau giam sat/ghi hinh; Admin; giang vien tao recording; giang vien khac | Bat/tat giam sat va ghi; kiem tra icon Client; thu truy cap recording theo role; kiem tra log va du lieu thu | Chi giam sat khi chu dong; icon/canh bao dung; video chi luu tren Server; Admin va dung giang vien duoc phep; khong ghi phim/am thanh/quet ngoai folder hoc tap; co audit log | `BLOCKED_BY_FND_02`, `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR08-IT-01 | BR-08 | IT, NT, MT | FND-03 metrics contract; file transfer va stream co san | File co dung luong biet truoc; heartbeat/video; cac may co RTT/loss khac nhau | Chay push/pull; doi chieu Mbps, phan tram, ETA; chay UDP; doi chieu RTT/loss/FPS/bandwidth | Don vi Mbps, MB/GB dung; metrics theo tung may/luong; % hoan thanh va ETA hop ly; RTT/loss/FPS/bandwidth duoc cap nhat | `BLOCKED_BY_FND_03`, `BLOCKED_BY_FEATURE` |
| BR09-IT-01 | BR-09 | IT, MT | FND-02 schema/DAO; auth/role feature; Server va Client | Tai khoan Admin/Giang vien/Sinh vien; sai mat khau; tai khoan khoa; Client chua login | Dang nhap Server theo role; thu cac thao tac duoc/khong duoc phep; quan ly tai khoan; mo Client khong login | Server yeu cau dang nhap; chi giang vien dieu khien lop; Admin quan ly tai khoan; sinh vien chi dinh danh Client; may chua login van online | `PASS` (phan Server login, SC_001 — xem muc 10); `BLOCKED_BY_FEATURE` (quan ly tai khoan, Client) |
| BR10-IT-01 | BR-10 | IT, MT | FND-02 recording schema/DAO; recording/auth feature | Recording qua han/con han; Admin; giang vien tao; giang vien khac | Tao recording; xem list/replay/xoa theo role; doi retention; chay cleanup; kiem tra audit log | Admin xem/xoa tat ca; giang vien chi xem recording cua minh; qua han tu xoa theo retention; log day du nguoi/thoi diem/hanh dong; mac dinh 30 ngay | `BLOCKED_BY_FND_02`, `BLOCKED_BY_FEATURE` |

## 7. Test cases FND-05 harness va PostgreSQL

| Test ID | BR ID | Loai | Tien dieu kien | Du lieu dau vao | Buoc thuc hien | Ket qua mong doi | Trang thai |
|---|---|---|---|---|---|---|---|
| FND05-UT-01 | N/A | UT | Fixture API kha dung | Timeout bang 0 ms | Goi FakeServer/FakeClient voi timeout khong hop le | Validation tu choi timeout truoc khi cho ket noi/IO | `PASS` |
| FND05-IT-01 | N/A | IT, NT | `shared-test` fixtures build duoc | TCP loopback va cong ephemeral | Khoi tao FakeServer/FakeClient, accept ket noi | Ket noi loopback duoc thiet lap; khong can protocol san xuat | `PASS` |
| FND05-IT-02 | N/A | IT, NT | Hai fixture da ket noi | Heartbeat, command, ACK test | Gui heartbeat; gui/nhan command; gui/nhan ACK; assert history hai dau | Payload va ACK duoc nhan/ghi nhan dung | `PASS` |
| FND05-NT-03 | N/A | NT | Socket da ket noi; timeout huu han | Khong gui du lieu; dong ket noi client/server | Goi accept/read khi qua han; dong client hoac server; doc EOF | Accept/read nem timeout trong gioi han; dong ket noi tu hai phia duoc nhan la EOF; test khong treo | `PASS` |
| FND05-IT-04 | N/A | IT | Fixture da tao socket | Dong FakeServer/FakeClient | Dong fixture va kiem tra trang thai socket | Socket/fixture duoc dong; khong ro ri tai nguyen | `PASS` |
| FND05-IT-05 | N/A | IT | FakeClient/FakeServer da build | Payload rong, chi whitespace hoac co CR/LF | Thu gui command, heartbeat va ACK khong hop le | Fixture tu choi truoc khi ghi message ra socket | `PASS` |
| FND05-IT-06 | N/A | IT | Hai fixture da ket noi | Message co prefix khong mong doi | Gui message sai loai va goi phuong thuc receive tuong ung | Fixture bao IOException, khong coi du lieu sai dinh dang la ACK/command/heartbeat hop le | `PASS` |
| FND05-DB-01 | N/A | IT | FND-02 migration va DAO that co san | Du lieu CRUD/rang buoc theo schema that | Chay migration trong Embedded PostgreSQL; CRUD, FK, constraint va rollback theo DAO/schema | Ket qua phu hop schema that; moi test co du lieu co lap | `BLOCKED_BY_FND_02` |
| FND05-DB-02 | N/A | IT | Khong can schema/DAO | `SELECT 1` | Khoi dong Zonky Embedded PostgreSQL; mo JDBC connection; chay query; dong tai nguyen | PostgreSQL embedded tra ket qua 1 va connection duoc dong sach | `PASS` |

## 8. Gioi han va dependency

- Tren nhanh FND-05 hien tai, migration/DAO FND-02 chua duoc tich hop. Remote `origin/feature/fnd-02-db-schema` co `db/migration/V1__schema.sql`, `V2__seed.sql`, cac DAO trong `modules/server/.../model/dao`, va `DatabaseMigrationIT`. Tuy nhien nhanh FND-02 dat Gradle module tai `modules/*`, khong trung layout root `shared/server/client/shared-test` cua nhanh nay; khong duoc copy/integrate code production FND-02 trong FND-05.
- FND-02 remote chua co auth service xac thuc password/session; `UserDao.findActiveByUsername()` va `StudentSessionDao` la DAO, khong phai API dang nhap. `DatabaseMigrationIT` co hau to `IT`, va server build khong khai bao task integration-test rieng; can owner FND-02/FND-06 xac nhan cach chay.
- Khong tao schema gia. CRUD/FK/constraint/rollback DAO tests tren nhanh hien tai van `BLOCKED_BY_FND_02`.
- FND-03 chua co protocol/network implementation trong workspace tai thoi diem lap ke hoach. Harness loopback la fixture doc lap; BR test can contract that va dang cho FND-03.
- Kiem thu multicast, UDP quality, Windows input blocking, file lon, MP4 va retention can implementation va/hoac phong may that; khong the xac nhan bang loopback harness.
- Cac test BR la ke hoach chua chay, khong phai ket qua nghiem thu. Cap nhat trang thai sau moi dot chay voi output Gradle thuc te.

## 9. Ket qua thuc thi

Ngay 2026-11-22, voi Java 21 toolchain:

| Lenh | Ket qua | Test |
|---|---|---|
| `gradlew.bat :shared-test:test --rerun-tasks` | BUILD SUCCESSFUL | 11 passed, 0 failed, 0 skipped (8 harness tests va 3 smoke tests hien co) |
| `gradlew.bat :server:test --rerun-tasks` | BUILD SUCCESSFUL | 1 passed, 0 failed, 0 skipped; bao gom `SELECT 1` tren Zonky Embedded PostgreSQL |
| `gradlew.bat build` | BUILD SUCCESSFUL | Full multi-module build; cac test da chay o hai lenh tren |

Tong test thuc thi: 12 passed, 0 failed, 0 skipped. Gradle co canh bao deprecated features, nhung khong anh huong ket qua build. Ket qua nay khong bao gom BR integration/network/manual tests hoac DAO tests dang bi chan.

## 10. Ket qua SC_001 (Dang nhap Server)

Ngay 2026-11-28, nhanh `feature/sc-001-server-login`:

| Lenh | Ket qua | Test |
|---|---|---|
| `gradlew.bat :shared:test` | BUILD SUCCESSFUL | 13 passed (them `PasswordHasherTest`: 5 case) |
| `gradlew.bat :server:test` | BUILD SUCCESSFUL | 46 passed, 0 failed |

Chi tiet test SC_001:

| Test class | So case | Noi dung |
|---|---|---|
| `JdbiLoginServiceIT` | 10 | PostgreSQL nhung that: teacher/admin dang nhap, trim username, sai mat khau, tai khoan khong ton tai, tai khoan khoa, tai khoan xoa mem, sinh vien bi tu choi tren Server, input rong, khong mo phien khi that bai |
| `LoginControllerTest` | 8 | Form that tu `login.fxml`: dieu huong theo role, thong bao loi tung truong hop, chan input rong truoc khi goi service, loi service -> SYSTEM_UNAVAILABLE, xoa password field, an loi o lan thu tiep theo, stylesheet nap duoc |
| `DashboardControllerTest` | 5 | Giang vien chi thay menu lop hoc; Admin chi thay menu quan tri; dang xuat; session hien hanh; muc dieu huong dau tien duoc chon |
| `LoginSessionRegistryTest` | 5 | Mo/tim/dong phien, id duy nhat, null/khong ton tai, closeAll |
| `LoginResultTest` | 5 | Success/rejected, bat buoc dung mot ket qua, moi ly do co thong bao, service unavailable |
| `ServerNavigatorTest` | 2 | Luong that: dang nhap thanh cong thay form bang dashboard dung role; dang nhap that bai giu form va hien loi |

Phan chua kiem thu duoc trong SC_001: thao tac nguoi dung that tren Windows (MT) va tich hop voi Client SC_002 (thuoc nhanh `feature/sc-002-client-login`).