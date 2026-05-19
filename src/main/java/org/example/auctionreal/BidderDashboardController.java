package org.example.auctionreal;

import database.dao.ItemDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import user.Bidder;
import user.User;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import database.dao.WatchlistDAO;

/**
 * BidderDashboardController: Xử lý màn hình dành cho Người Mua (Bidder).
 * Cho phép Bidder tìm kiếm, duyệt vật phẩm đang đấu giá và tham gia phiên đấu
 * giá.
 */
public class BidderDashboardController {

    // ===== Các phần tử UI (phải khớp fx:id trong bidder-dashboard.fxml) =====

    // Thanh trên
    @FXML
    private Label lblUserInfo;
    @FXML
    private Label lblBalance;

    // Thanh tìm kiếm & bộ lọc
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbFilterCategory;
    @FXML
    private ComboBox<String> cmbFilterPrice;
    @FXML
    private ComboBox<String> cmbFilterStatus;

    // Thống kê nhanh
    @FXML
    private Label lblLiveCount;
    @FXML
    private Label lblEndingSoon;
    @FXML
    private Label lblLeading;
    @FXML
    private Label lblTotalItems;

    // Danh sách vật phẩm
    @FXML
    private ListView<String> listAuctionItems;
    @FXML
    private ComboBox<String> cmbSort;
    @FXML
    private Label lblPage;

    // Panel chi tiết
    @FXML
    private Label lblStatusBadge;
    @FXML
    private Label lblSessionId;
    @FXML
    private Label lblItemName;
    @FXML
    private Label lblSellerName;
    @FXML
    private Label lblItemEmoji;
    @FXML
    private Label lblItemCategory;
    @FXML
    private Label lblDescription;
    @FXML
    private HBox hboxTags;
    @FXML
    private Label lblCurrentPrice;
    @FXML
    private Label lblTopBidder;
    @FXML
    private Label lblCountdown;
    @FXML
    private Button btnJoinAuction;
    @FXML
    private Button btnWatchlist;

    // ===== Dữ liệu nội bộ =====
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /** Danh sách vật phẩm demo (sẽ thay bằng dữ liệu từ database sau). */
    private final List<AuctionItemDemo> demoItems = new ArrayList<>();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    // ===== Khởi tạo =====

    @FXML
    public void initialize() {
        // Hiển thị thông tin người dùng & số dư
        User user = RegisterController.currentUser;
        if (user != null) {
            lblUserInfo.setText("👤 " + user.getUsername() + "  |  BIDDER");
            if (user instanceof Bidder bidder) {
                lblBalance.setText(currencyFormat.format((long) bidder.getBalance()) + " ₫");
            } else {
                lblBalance.setText("---");
            }
        } else {
            lblUserInfo.setText("👤 Khách");
            lblBalance.setText("---");
        }

        // Đổ dữ liệu bộ lọc
        cmbFilterCategory.getItems().addAll("Tất cả", "Đồng hồ", "Điện tử", "Trang sức", "Xe cộ", "Nghệ thuật",
                "Thời trang", "Khác");
        cmbFilterCategory.setValue("Tất cả");

        cmbFilterPrice.getItems().addAll("Tất cả", "Dưới 10 triệu", "10 - 100 triệu", "100 - 500 triệu",
                "Trên 500 triệu");
        cmbFilterPrice.setValue("Tất cả");

        cmbFilterStatus.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp kết thúc (30 phút)", "Sắp bắt đầu");
        cmbFilterStatus.setValue("Tất cả");

        cmbSort.getItems().addAll("Mới nhất", "Giá thấp → cao", "Giá cao → thấp", "Kết thúc sớm nhất");
        cmbSort.setValue("Mới nhất");

        // Load dữ liệu từ Database (fallback sang demo nếu DB trống)
        loadItemsFromDatabase();
        refreshItemList();
        refreshStats();
    }

    // ===== XỬ LÝ SỰ KIỆN =====

    /**
     * handleSearch: Tìm kiếm và lọc danh sách vật phẩm.
     */
    @FXML
    void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().trim().toLowerCase();
        listAuctionItems.getItems().clear();
        for (AuctionItemDemo item : demoItems) {
            boolean matchKeyword = keyword.isEmpty() || item.name.toLowerCase().contains(keyword);
            if (matchKeyword) {
                listAuctionItems.getItems().add(formatItemForList(item));
            }
        }
        lblPage.setText("Trang 1 / 1");
    }

    /**
     * handleItemSelected: Khi người dùng nhấp vào một vật phẩm trong danh sách.
     */
    @FXML
    void handleItemSelected(javafx.scene.input.MouseEvent event) {
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= demoItems.size())
            return;

        AuctionItemDemo item = demoItems.get(idx);
        updateDetailPanel(item);
    }

    /**
     * handleJoinAuction: Truyền dữ liệu vật phẩm được chọn vào AuctionController,
     * sau đó chuyển sang màn hình đấu giá auction.fxml.
     */
    @FXML
    void handleJoinAuction(ActionEvent event) {
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0)
            return;

        AuctionItemDemo item = demoItems.get(idx);

        // ---- Truyền dữ liệu vật phẩm vào AuctionController ----
        AuctionController.selectedName = item.name;
        AuctionController.selectedSubtitle = item.seller + "  •  " + item.condition;
        AuctionController.selectedEmoji = item.emoji;
        AuctionController.selectedBrand = item.name.toUpperCase();
        AuctionController.selectedDescription = item.description;
        AuctionController.selectedStartPrice = item.currentPrice;
        AuctionController.selectedMinStep = 1_000_000.0; // mặc định 1M, có thể mở rộng sau
        AuctionController.selectedDuration = 2; // mặc định 2 phút, có thể mở rộng sau

        // ---- Chuyển sang trang đấu giá ----
        try {
            Parent root = FXMLLoader.load(getClass().getResource("auction.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 700));
            stage.setMinWidth(900);
            stage.setMinHeight(700);
            stage.setMaximized(true);
            stage.setTitle("⚡ Đấu Giá LIVE – " + item.name);
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi chuyển sang auction.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * handleWatchlist: Thêm / bỏ vật phẩm khỏi danh sách theo dõi.
     */
    @FXML
    void handleWatchlist(ActionEvent event) {

        User user =
                RegisterController.currentUser;

        if (user == null) {

            return;
        }

        int userId =
                Integer.parseInt(
                        user.getId()
                );

        WatchlistDAO watchlistDAO =
                new WatchlistDAO();

        boolean success =

                watchlistDAO.addToWatchlist(
                        userId,
                        AuctionController.selectedAuctionId
                );

        if (success) {

            btnWatchlist.setText(
                    "✅ Đã thêm theo dõi"
            );

        } else {

            btnWatchlist.setText(
                    "❌ Lỗi watchlist"
            );
        }
    }

    /**
     * handleMyBids: Xem lịch sử đặt giá của bản thân.
     */
    @FXML
    void handleMyBids(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Lịch sử đặt giá");
        info.setHeaderText("📋 Lịch sử đặt giá của bạn");
        info.setContentText(
                "Tính năng này sẽ hiển thị toàn bộ lịch sử đặt giá của bạn.\n(Kết nối database trong phiên bản tiếp theo)");
        info.showAndWait();
    }

    /** handlePrevPage / handleNextPage: Phân trang danh sách. */
    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            refreshItemList();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        int totalPages = Math.max(1, (int) Math.ceil((double) demoItems.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            refreshItemList();
        }
    }

    /**
     * handleFilter: Không dùng trực tiếp (ComboBox dùng handleSearch). Dự phòng.
     */
    @FXML
    void handleFilter(ActionEvent event) {
        handleSearch(event);
    }

    /**
     * handleBack: Quay về màn hình chọn vai trò.
     */
    @FXML
    void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(false);
        stage.setScene(new Scene(root, 600, 400));
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setTitle("Lựa chọn vai trò");
        stage.show();
    }

    // ===== TIỆN ÍCH PRIVATE =====

    // ===== NẠP DỮ LIỆU =====

    /**
     * Load vật phẩm từ Database.
     * Nếu DB trống hoặc không kết nối được, tự động dùng dữ liệu demo.
     */
    private void loadItemsFromDatabase() {
        demoItems.clear();
        ItemDAO itemDAO = new ItemDAO();
        List<String> dbItems = itemDAO.getAllItemsFormatted();

        if (!dbItems.isEmpty()) {
            // Dữ liệu từ DB: chuyển sang AuctionItemDemo đơn giản
            for (String line : dbItems) {
                // Format: "🏷 name | price ₫ | seller | status"
                String[] parts = line.split("  \\|  ");
                String name   = parts.length > 0 ? parts[0].replace("🏷 ", "").trim() : "?";
                String seller = parts.length > 2 ? parts[2].replace("👤 ", "").trim() : "Hệ thống";
                demoItems.add(new AuctionItemDemo(
                        name, seller,
                        "(Mô tả từ database)",
                        0, "Khác", "🏷", "--", "--:--:--"));
            }
            // Đồng thời cập nhật ListView trực tiếp với chuỗi đầy đủ từ DB
            listAuctionItems.getItems().clear();
            listAuctionItems.getItems().addAll(dbItems);
        } else {
            // Fallback: dùng demo nếu DB chưa có dữ liệu
            System.out.println("[BidderDashboard] DB trống hoặc chưa kết nối, dùng demo data.");
            loadDemoItems();
        }
    }

    /**
     * Load dữ liệu demo. Dùng khi DB chưa sẵn sàng.
     */
    private void loadDemoItems() {
        demoItems.add(new AuctionItemDemo("Rolex Submariner Date 2023", "Nguyen Hoang Anh",
                "Đồng hồ lặn biểu tượng, bezel ceramic đen, tình trạng mới 98%.",
                285_000_000, "Đồng hồ", "🕰", "Như mới", "01:45:30"));
        demoItems.add(new AuctionItemDemo("iPhone 15 Pro Max 256GB", "Le Dinh Bach",
                "Máy nguyên seal, màu titan tự nhiên, bảo hành Apple 12 tháng.",
                35_000_000, "Điện tử", "📱", "Mới 100%", "00:30:00"));
        demoItems.add(new AuctionItemDemo("Nhẫn kim cương 2 carat GIA", "Nguyen Danh Hai",
                "Kim cương thiên nhiên, chứng nhận GIA, vàng trắng 18K.",
                450_000_000, "Trang sức", "💍", "Mới 100%", "03:00:00"));
        demoItems.add(new AuctionItemDemo("Toyota Land Cruiser 2022", "Le Dinh Bach",
                "ODO 15.000km, xe nhập khẩu chính hãng, đầy đủ giấy tờ.",
                4_500_000_000L, "Xe cộ", "🚙", "Như mới", "02:15:00"));
        demoItems.add(new AuctionItemDemo("Tranh sơn dầu cổ điển (1895)", "Nguyen Minh Hieu",
                "Tranh gốc thế kỷ 19, có chứng chỉ xác thực, kích thước 80x120cm.",
                125_000_000, "Nghệ thuật", "🖼", "Cổ vật", "05:00:00"));
    }

    /** Làm mới danh sách trong ListView theo trang hiện tại. */
    private void refreshItemList() {
        listAuctionItems.getItems().clear();
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, demoItems.size());
        for (int i = start; i < end; i++) {
            listAuctionItems.getItems().add(formatItemForList(demoItems.get(i)));
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) demoItems.size() / PAGE_SIZE));
        lblPage.setText("Trang " + currentPage + " / " + totalPages);
    }

    /** Làm mới các ô thống kê nhanh. */
    private void refreshStats() {
        lblTotalItems.setText(String.valueOf(demoItems.size()));
        lblLiveCount.setText(String.valueOf(demoItems.size())); // demo: tất cả đều live
        lblEndingSoon.setText("2"); // demo cứng
        lblLeading.setText("0");
    }

    /** Tạo chuỗi hiển thị gọn cho một vật phẩm trong ListView. */
    private String formatItemForList(AuctionItemDemo item) {
        return String.format("%s %s  |  %s ₫  |  ⏱ %s",
                item.emoji,
                item.name,
                currencyFormat.format((long) item.currentPrice),
                item.countdown);
    }

    /** Cập nhật panel chi tiết khi người dùng chọn vật phẩm. */
    private void updateDetailPanel(AuctionItemDemo item) {
        lblItemName.setText(item.name);
        lblSellerName.setText("Người bán: " + item.seller);
        lblItemEmoji.setText(item.emoji);
        lblItemCategory.setText(item.category);
        lblDescription.setText(item.description);
        lblCurrentPrice.setText(currencyFormat.format((long) item.currentPrice) + " ₫");
        lblTopBidder.setText("👑 Chưa có ai dẫn đầu");
        lblCountdown.setText(item.countdown);
        lblSessionId.setText("Phiên #" + (demoItems.indexOf(item) + 1001));

        // Cập nhật tag trạng thái
        hboxTags.getChildren().clear();
        Label tagCondition = new Label(item.condition);
        tagCondition.setStyle(
                "-fx-text-fill: #4af0a0; -fx-background-color: #0a2a1a; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        Label tagCat = new Label(item.category);
        tagCat.setStyle(
                "-fx-text-fill: #4ab0f0; -fx-background-color: #0a1a2a; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        hboxTags.getChildren().addAll(tagCondition, tagCat);

        // Bật nút tham gia
        btnJoinAuction.setDisable(false);
        btnWatchlist.setText("🔖  Thêm vào danh sách theo dõi");
        btnWatchlist.setStyle(
                "-fx-background-color: #1e1e3a; -fx-text-fill: #aaaacc; -fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    // ===== DATA CLASS =====

    /**
     * AuctionItemDemo: Lưu thông tin một vật phẩm đấu giá (demo).
     * Sau này sẽ thay bằng class model chính thức kết nối database.
     */
    private static class AuctionItemDemo {
        String name, seller, description, category, emoji, condition, countdown;
        double currentPrice;

        AuctionItemDemo(String name, String seller, String description,
                double currentPrice, String category,
                String emoji, String condition, String countdown) {
            this.name = name;
            this.seller = seller;
            this.description = description;
            this.currentPrice = currentPrice;
            this.category = category;
            this.emoji = emoji;
            this.condition = condition;
            this.countdown = countdown;
        }
    }
}
