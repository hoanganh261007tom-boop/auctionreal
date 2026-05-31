# 🏆 AuctionReal – Hệ thống Đấu Giá Trực Tuyến
nhóm 12 

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

## 4. Hướng dẫn Build Executable Fat JAR

Dự án sử dụng `maven-shade-plugin` được cấu hình trong `pom.xml` để tự động đóng gói tất cả mã nguồn và các thư viện phụ thuộc (JavaFX, MySQL Connector, ControlsFX,...) vào một file JAR duy nhất (Fat JAR / Uber JAR).

### Cách build:
Mở terminal tại thư mục gốc của dự án và chạy lệnh sau:

* **Trên Windows:**
  ```cmd
  .\mvnw.cmd clean package
  ```
* **Trên macOS / Linux:**
  ```bash
  chmod +x mvnw
  ./mvnw clean package
  ```
* **Hoặc nếu máy đã cài sẵn Maven:**
  ```bash
  mvn clean package
  ```

Sau khi build thành công, file JAR đóng gói đầy đủ sẽ được tạo ra tại:
```
target/auctionreal-1.0-SNAPSHOT.jar
```

---

## 5. Hướng dẫn chạy chương trình

### Bước 1: Thiết lập Cơ sở dữ liệu (MySQL)
1. Khởi động MySQL Server của bạn.
2. Mở công cụ quản lý cơ sở dữ liệu (ví dụ: **MySQL Workbench** hoặc **DBeaver**).
3. Mở file `init.sql` nằm ở thư mục gốc của dự án và thực thi (Execute) toàn bộ script để tạo database `auctiondb_local` và các bảng dữ liệu mẫu.

### Bước 2: Cấu hình thông tin kết nối Cơ sở dữ liệu
Nếu tài khoản hoặc mật khẩu MySQL của bạn khác cấu hình mặc định, hãy cập nhật trước khi build:
1. Mở file [DatabaseConnection.java](file:///c:/Users/PV/Documents/BTL/auctionreal/src/main/java/database/DatabaseConnection.java).
2. Tìm dòng 21 và chỉnh sửa mật khẩu MySQL của bạn:
   ```java
   private static final String PASSWORD = "your_mysql_password";
   ```

### Bước 3: Khởi chạy ứng dụng bằng Executable JAR

Bạn có thể chạy toàn bộ hệ thống trực tiếp từ dòng lệnh thông qua file `.jar` đã build:

#### Cách 1: Khởi chạy nhanh (Được khuyến khích)
Chỉ cần chạy lệnh sau trên terminal để khởi chạy cả **Server** (chạy ngầm trên port `9999`) và **Client thứ nhất** (giao diện đăng nhập):
```bash
java -jar target/auctionreal-1.0-SNAPSHOT.jar
```

* Để mở thêm các **Client thứ 2, thứ 3...** nhằm kiểm thử tính năng đấu giá realtime giữa nhiều người dùng, bạn mở các tab terminal mới và chạy tiếp lệnh trên:
  ```bash
  java -jar target/auctionreal-1.0-SNAPSHOT.jar
  ```
  *(Server sẽ phát hiện cổng `9999` đã bị chiếm dụng bởi client đầu tiên, thông báo bỏ qua việc khởi động server mới và tiếp tục mở thêm cửa sổ giao diện Client một cách mượt mà).*

---

#### Cách 2: Khởi chạy Server và Client riêng biệt

Nếu bạn muốn tách biệt quá trình giám sát Server và Client trên các cửa sổ terminal khác nhau:

1. **Khởi chạy độc lập Auction Server:**
   ```bash
   java -cp target/auctionreal-1.0-SNAPSHOT.jar org.example.auctionreal.network.AuctionServer
   ```
   *Server sẽ chạy và lắng nghe kết nối từ các client trên port `9999`.*

2. **Khởi chạy các cửa sổ Client (JavaFX UI):**
   Mở một hoặc nhiều terminal khác và chạy lệnh sau để khởi động Client kết nối đến Server:
   ```bash
   java -cp target/auctionreal-1.0-SNAPSHOT.jar org.example.auctionreal.HelloApplication
   ```

### Bước 4: Tài khoản đăng nhập mẫu
Bạn có thể đăng nhập bằng các tài khoản có sẵn trong cơ sở dữ liệu mẫu:

| Username | Password | Vai trò | Mô tả |
|----------|----------|---------|-------|
| `admin` | `admin123` | **Admin** | Quản lý người dùng, xem danh sách sản phẩm |
| `seller1` | `123456` | **Seller** | Người bán: Đăng sản phẩm mới, sửa/xóa sản phẩm của mình |
| `bidder1` | `123456` | **Bidder** | Người mua: Tìm kiếm, theo dõi, đấu giá thủ công / tự động |
| `bidder2` | `123456` | **Bidder** | Người mua thứ 2 (để test đấu giá đồng thời) |


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
| 📄 Báo cáo PDF | https://drive.google.com/file/d/1I3BRYa1FCyJF_T_SY1HIMXtXRZ5x1xUt/view?usp=sharing |
| 🎬 Video Demo | https://drive.google.com/file/d/1y66vFxooQtfbFE7DjyDeHPTMjQ_Fimls/view?usp=sharing |

---

