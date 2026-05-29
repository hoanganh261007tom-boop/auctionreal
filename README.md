# 🏆 AuctionReal – Hệ thống Đấu Giá Trực Tuyến
nhóm 1

---

## 1. Mô tả bài toán

AuctionReal là hệ thống đấu giá trực tuyến cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua sản phẩm trong một khoảng thời gian xác định. Thay vì bán với giá cố định, người bán đưa sản phẩm lên hệ thống và giá bán cuối cùng được xác định thông qua quá trình đấu giá giữa các người mua.

**Phạm vi hệ thống:**
- Người bán (Seller) đăng sản phẩm, sửa/xóa sản phẩm
- Người mua (Bidder) xem danh sách, tham gia đấu giá, đặt giá thủ công hoặc tự động
- Quản trị viên (Admin) quản lý toàn bộ người dùng và sản phẩm
- Hệ thống tự động đóng phiên, xác định người thắng, cập nhật realtime qua Socket

---

## 2. Công nghệ sử dụng

| Công nghệ | Mục đích |
|-----------|----------|
| **Java 21** | Ngôn ngữ lập trình chính |
| **JavaFX 21** | Giao diện đồ họa (GUI) |
| **MySQL 8.0** | Cơ sở dữ liệu |
| **JDBC** | Kết nối Java với MySQL |
| **TCP Socket** | Giao tiếp realtime giữa Client và Server |
| **Maven** | Quản lý build và dependency |
| **JUnit 5** | Unit Testing |

**Môi trường chạy:**
- Hệ điều hành: Windows 10/11, macOS, Linux
- JDK: 21 trở lên
- RAM: tối thiểu 4GB
- MySQL Server: 8.0 trở lên

**Yêu cầu cài đặt:**
- [JDK 21](https://www.oracle.com/java/technologies/downloads/)
- [MySQL 8.0](https://dev.mysql.com/downloads/installer/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)

---

## 3. Cấu trúc thư mục

```
auctionreal/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── database/
│   │   │   │   ├── DatabaseConnection.java     # Singleton – kết nối DB
│   │   │   │   ├── UserDAO.java                # Quản lý người dùng
│   │   │   │   ├── dao/
│   │   │   │   │   ├── AuctionDAO.java         # Quản lý phiên đấu giá
│   │   │   │   │   ├── BidDAO.java             # Đặt giá (synchronized)
│   │   │   │   │   ├── ItemDAO.java            # Quản lý sản phẩm
│   │   │   │   │   └── WatchlistDAO.java       # Danh sách theo dõi
│   │   │   │   └── model/
│   │   │   │       ├── Entity.java             # Abstract base class
│   │   │   │       ├── Item.java               # Abstract class sản phẩm
│   │   │   │       ├── ElectronicsItem.java    # Sản phẩm điện tử
│   │   │   │       ├── ArtItem.java            # Tác phẩm nghệ thuật
│   │   │   │       ├── VehicleItem.java        # Phương tiện
│   │   │   │       ├── GeneralItem.java        # Sản phẩm thông thường
│   │   │   │       └── ItemFactory.java        # Factory Method Pattern
│   │   │   ├── user/
│   │   │   │   ├── User.java                   # Abstract class người dùng
│   │   │   │   ├── Bidder.java                 # Người đấu giá
│   │   │   │   ├── Seller.java                 # Người bán
│   │   │   │   └── Admin.java                  # Quản trị viên
│   │   │   └── org/example/auctionreal/
│   │   │       ├── network/
│   │   │       │   ├── AuctionServer.java      # TCP Socket Server
│   │   │       │   ├── ClientHandler.java      # Xử lý từng client
│   │   │       │   └── SocketClient.java       # Client kết nối server
│   │   │       ├── observer/
│   │   │       │   ├── BidObserver.java        # Observer interface
│   │   │       │   └── AuctionEventManager.java# Observable + Singleton
│   │   │       ├── HelloController.java        # Màn hình đăng nhập
│   │   │       ├── RegisterController.java     # Màn hình đăng ký
│   │   │       ├── RoleSelectionController.java# Chọn vai trò
│   │   │       ├── AuctionController.java      # Màn hình đấu giá
│   │   │       ├── BidderDashboardController.java
│   │   │       ├── SellerDashboardController.java
│   │   │       └── AdminDashboardController.java
│   │   └── resources/
│   │       └── org/example/auctionreal/
│   │           ├── hello-view.fxml             # Giao diện đăng nhập
│   │           ├── register.fxml               # Giao diện đăng ký
│   │           ├── role-selection.fxml         # Giao diện chọn vai trò
│   │           ├── auction.fxml                # Giao diện đấu giá
│   │           ├── bidder-dashboard.fxml       # Dashboard người mua
│   │           ├── seller-dashboard.fxml       # Dashboard người bán
│   │           └── admin-dashboard.fxml        # Dashboard admin
│   └── test/
│       └── java/org/example/auctionreal/
│           └── AuctionSystemTest.java          # Unit Tests (JUnit 5)
├── init.sql                                    # Script tạo database
├── pom.xml                                     # Maven config
└── README.md
```

---

## 4. Vị trí file JAR

Sau khi build bằng Maven, file `.jar` được tạo tại:

```
target/auctionreal-1.0-SNAPSHOT.jar
```

Để build:
```bash
mvn clean package
```

> ⚠️ File JAR chưa được đính kèm trong repository. Vui lòng build từ source code theo hướng dẫn bên dưới.

---

## 5. Hướng dẫn chạy theo thứ tự

### Bước 1: Tạo Database
Mở **MySQL Workbench** → `File → Open SQL Script` → chọn `init.sql` → bấm **Execute**

### Bước 2: Sửa mật khẩu Database
Mở `src/main/java/database/DatabaseConnection.java`, sửa dòng:
```java
private static final String PASSWORD = "260707"; // ← đổi thành password MySQL của bạn
```

### Bước 3: Khởi động Server
Trong IntelliJ, chạy class:
```
org.example.auctionreal.network.AuctionServer
```
Server khởi động trên **port 9999**. Giữ cửa sổ này mở.

### Bước 4: Khởi động Client
Chạy class:
```
org.example.auctionreal.Launcher
```
Giao diện đăng nhập sẽ hiện ra.

### Bước 5: Đăng nhập
Dùng tài khoản mẫu hoặc đăng ký mới:

| Username | Password | Vai trò |
|----------|----------|---------|
| `admin` | `admin123` | Admin |
| `seller1` | `123456` | Seller |
| `bidder1` | `123456` | Bidder |

> 💡 Có thể chạy nhiều cửa sổ Client cùng lúc để test đấu giá đồng thời.

---

## 6. Danh sách chức năng đã hoàn thành

### Chức năng bắt buộc ✅
- [x] Đăng ký / Đăng nhập tài khoản
- [x] Phân vai trò: Bidder, Seller, Admin
- [x] Seller: Thêm / Sửa / Xóa sản phẩm
- [x] Bidder: Xem danh sách phiên đấu giá
- [x] Bidder: Đặt giá, kiểm tra tính hợp lệ
- [x] Cập nhật người dẫn đầu phiên đấu giá
- [x] Tự động đóng phiên khi hết thời gian
- [x] Xác định người thắng cuộc
- [x] Xử lý lỗi: bid thấp hơn giá hiện tại, phiên đã đóng, lỗi kết nối
- [x] Giao diện JavaFX với đầy đủ các màn hình
- [x] Admin: Quản lý người dùng và sản phẩm
- [x] Thiết kế OOP: Entity → Item/User → các subclass
- [x] Design Pattern: Singleton, Factory Method, Observer
- [x] Xử lý đấu giá đồng thời: `synchronized` + `SELECT FOR UPDATE`
- [x] Realtime update qua TCP Socket
- [x] Kiến trúc Client–Server rõ ràng
- [x] MVC: JavaFX + FXML (client), Controller–DAO–DB (server)
- [x] Maven build tool
- [x] Unit Test với JUnit 5

### Chức năng nâng cao ✅
- [x] Auto Bidding: tự động đặt giá đến mức tối đa
- [x] Anti-sniping: gia hạn 60 giây nếu có bid trong 30 giây cuối
- [x] Watchlist: theo dõi phiên đấu giá yêu thích

---

## 7. Báo cáo & Video Demo

| Tài liệu | Link |
|----------|------|
| 📄 Báo cáo PDF | *(Sẽ cập nhật sau)* |
| 🎬 Video Demo | *(Sẽ cập nhật sau)* |

---

