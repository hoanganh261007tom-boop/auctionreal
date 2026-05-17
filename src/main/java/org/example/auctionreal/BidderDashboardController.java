package org.example.auctionreal;

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

/**
 * BidderDashboardController: Xử lý màn hình dành cho Người Mua (Bidder).
 */
public class BidderDashboardController {

    // ===== Hằng số & dữ liệu =====
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ===== Demo data =====
    private final List<AuctionItemDemo> demoItems = new ArrayList<>(List.of(
            new AuctionItemDemo("Rolex Submariner 2023", "LuxuryWatch",
                    "Đồng hồ Rolex chính hãng, còn bảo hành 2 năm.", 450_000_000,
                    "Đồng hồ", "⌚", "Như mới", "2:45:00"),
            new AuctionItemDemo("iPhone 15 Pro Max 256GB", "TechStore",
                    "iPhone 15 Pro Max fullbox, chưa active.", 29_000_000,
                    "Điện tử", "📱", "Mới 100%", "1:20:00"),
            new AuctionItemDemo("Kim cương 2 carat GIA", "JewelHouse",
                    "Kim cương thiên nhiên, chứng chỉ GIA.", 320_000_000,
                    "Trang sức", "💎", "Mới 100%", "0:55:00"),
            new AuctionItemDemo("Porsche 911 Carrera 2022", "LuxuryCar",
                    "Xe siêu sang, chạy 5000km.", 6_800_000_000.0,
                    "Xe cộ", "🏎", "Như mới", "3:10:00"),
            new AuctionItemDemo("Tranh sơn dầu Nguyễn Gia Trí", "ArtGallery",
                    "Tác phẩm nghệ thuật độc bản, có giấy chứng nhận.", 1_200_000_000.0,
                    "Nghệ thuật", "🎨", "Tốt", "4:30:00"),
            new AuctionItemDemo("Túi Hermès Birkin 30cm", "FashionHouse",
                    "Birkin hàng xịn, full phụ kiện.", 550_000_000,
                    "Thời trang", "👜", "Như mới", "1:05:00"),
            new AuctionItemDemo("MacBook Pro M3 Max 16\"", "AppleStore",
                    "MacBook Pro M3 Max, 36GB RAM, 1TB SSD.", 85_000_000,
                    "Điện tử", "💻", "Mới 100%", "0:40:00"),
            new AuctionItemDemo("Đồng hồ Patek Philippe Nautilus", "WatchMaster",
                    "Patek Philippe Nautilus thép không gỉ.", 1_800_000_000.0,
                    "Đồng hồ", "⌚", "Như mới", "2:15:00")
    ));

    // ===== FXML Fields – Thanh trên =====
    @FXML private Label lblUserInfo;
    @FXML private Label lblBalance;

    // ===== FXML Fields – Thống kê =====
    @FXML private Label lblLiveCount;
    @FXML private Label lblEndingSoon;
    @FXML private Label lblLeading;
    @FXML private Label lblTotalItems;

    // ===== FXML Fields – Tìm kiếm & bộ lọc =====
    @FXML private TextField         txtSearch;
    @FXML private ComboBox<String>  cmbFilterCategory;
    @FXML private ComboBox<String>  cmbFilterPrice;
    @FXML private ComboBox<String>  cmbFilterStatus;
    @FXML private ComboBox<String>  cmbSort;

    // ===== FXML Fields – Danh sách & phân trang =====
    @FXML private ListView<String>  listAuctionItems;
    @FXML private Label             lblPage;

    // ===== FXML Fields – Panel chi tiết (khớp đúng fx:id trong FXML) =====
    @FXML private Label             lblItemName;       // fx:id="lblItemName"
    @FXML private Label             lblSellerName;     // fx:id="lblSellerName"
    @FXML private Label             lblItemEmoji;      // fx:id="lblItemEmoji"
    @FXML private Label             lblItemCategory;   // fx:id="lblItemCategory"
    @FXML private Label             lblDescription;    // fx:id="lblDescription"
    @FXML private Label             lblCurrentPrice;   // fx:id="lblCurrentPrice"
    @FXML private Label             lblTopBidder;      // fx:id="lblTopBidder"
    @FXML private Label             lblCountdown;      // fx:id="lblCountdown"
    @FXML private Label             lblStatusBadge;    // fx:id="lblStatusBadge"
    @FXML private Label             lblSessionId;      // fx:id="lblSessionId"
    @FXML private Button            btnJoinAuction;    // fx:id="btnJoinAuction"
    @FXML private Button            btnWatchlist;      // fx:id="btnWatchlist"

    @FXML
    public void initialize() {
        // Hiển thị thông tin người dùng & số dư
        User user = RegisterController.currentUser;
        if (user != null) {
            if (lblUserInfo != null) lblUserInfo.setText("👤 " + user.getUsername() + "  |  BIDDER");
            if (user instanceof Bidder bidder) {
                if (lblBalance != null) lblBalance.setText(currencyFormat.format((long) bidder.getBalance()) + " ₫");
            } else {
                if (lblBalance != null) lblBalance.setText("---");
            }
        } else {
            if (lblUserInfo != null) lblUserInfo.setText("👤 Khách");
            if (lblBalance  != null) lblBalance.setText("---");
        }

        // Đổ dữ liệu bộ lọc
        if (cmbFilterCategory != null) {
            cmbFilterCategory.getItems().addAll("Tất cả", "Đồng hồ", "Điện tử", "Trang sức", "Xe cộ", "Nghệ thuật",
                    "Thời trang", "Khác");
            cmbFilterCategory.setValue("Tất cả");
        }

        if (cmbFilterPrice != null) {
            cmbFilterPrice.getItems().addAll("Tất cả", "Dưới 10 triệu", "10 - 100 triệu", "100 - 500 triệu",
                    "Trên 500 triệu");
            cmbFilterPrice.setValue("Tất cả");
        }

        if (cmbFilterStatus != null) {
            cmbFilterStatus.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp kết thúc (30 phút)", "Sắp bắt đầu");
            cmbFilterStatus.setValue("Tất cả");
        }

        if (cmbSort != null) {
            cmbSort.getItems().addAll("Mới nhất", "Giá thấp → cao", "Giá cao → thấp", "Kết thúc sớm nhất");
            cmbSort.setValue("Mới nhất");
        }

        refreshItemList();
        refreshStats();
    }

    // ===== XỬ LÝ SỰ KIỆN =====

    @FXML
    void handleSearch(ActionEvent event) {
        String keyword = (txtSearch != null) ? txtSearch.getText().trim().toLowerCase() : "";
        if (listAuctionItems != null) {
            listAuctionItems.getItems().clear();
            for (AuctionItemDemo item : demoItems) {
                boolean matchKeyword = keyword.isEmpty() || item.name.toLowerCase().contains(keyword);
                if (matchKeyword) {
                    listAuctionItems.getItems().add(formatItemForList(item));
                }
            }
        }
        if (lblPage != null) lblPage.setText("Trang 1 / 1");
    }

    @FXML
    void handleItemSelected(javafx.scene.input.MouseEvent event) {
        if (listAuctionItems == null) return;
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= demoItems.size()) return;
        AuctionItemDemo item = demoItems.get(idx);
        updateDetailPanel(item);
    }

    /**
     * handleJoinAuction: Truyền dữ liệu vật phẩm được chọn vào AuctionController,
     * sau đó chuyển sang màn hình đấu giá auction.fxml.
     */
    @FXML
    void handleJoinAuction(ActionEvent event) {
        if (listAuctionItems == null) return;
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;

        AuctionItemDemo item = demoItems.get(idx);

        // ---- Truyền dữ liệu vật phẩm vào AuctionController ----
        AuctionController.selectedName        = item.name;
        AuctionController.selectedSubtitle    = item.seller + "  •  " + item.condition;
        AuctionController.selectedEmoji       = item.emoji;
        AuctionController.selectedBrand       = item.name.toUpperCase();
        AuctionController.selectedDescription = item.description;
        AuctionController.selectedStartPrice  = item.currentPrice;
        AuctionController.selectedMinStep     = 1_000_000.0;
        AuctionController.selectedDuration    = 2;

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

    @FXML
    void handleWatchlist(ActionEvent event) {
        if (listAuctionItems == null) return;
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        if (btnWatchlist != null) {
            btnWatchlist.setText("✅  Đã thêm vào danh sách theo dõi");
            btnWatchlist.setStyle(
                    "-fx-background-color: #0a2a1a; -fx-text-fill: #4af0a0; -fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    @FXML
    void handleMyBids(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Lịch sử đặt giá");
        info.setHeaderText("📋 Lịch sử đặt giá của bạn");
        info.setContentText(
                "Tính năng này sẽ hiển thị toàn bộ lịch sử đặt giá của bạn.\n(Kết nối database trong phiên bản tiếp theo)");
        info.showAndWait();
    }

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

    @FXML
    void handleFilter(ActionEvent event) {
        handleSearch(event);
    }

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

    private void refreshItemList() {
        if (listAuctionItems == null) return;
        listAuctionItems.getItems().clear();
        int start = (currentPage - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, demoItems.size());
        for (int i = start; i < end; i++) {
            listAuctionItems.getItems().add(formatItemForList(demoItems.get(i)));
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) demoItems.size() / PAGE_SIZE));
        if (lblPage != null) lblPage.setText("Trang " + currentPage + " / " + totalPages);
    }

    private String formatItemForList(AuctionItemDemo item) {
        return String.format("%s  %s  |  %s ₫  |  ⏱ %s",
                item.emoji, item.name,
                currencyFormat.format((long) item.currentPrice),
                item.countdown);
    }

    private void updateDetailPanel(AuctionItemDemo item) {
        if (lblItemName    != null) lblItemName.setText(item.emoji + "  " + item.name);
        if (lblSellerName  != null) lblSellerName.setText("🏪 " + item.seller);
        if (lblItemEmoji   != null) lblItemEmoji.setText(item.emoji);
        if (lblItemCategory!= null) lblItemCategory.setText(item.category);
        if (lblDescription != null) lblDescription.setText(item.description);
        if (lblCurrentPrice!= null) lblCurrentPrice.setText(currencyFormat.format((long) item.currentPrice) + " ₫");
        if (lblTopBidder   != null) lblTopBidder.setText("Chưa có ai đặt giá");
        if (lblCountdown   != null) lblCountdown.setText(item.countdown);
        if (lblStatusBadge != null) lblStatusBadge.setText("● ĐANG ĐẤU GIÁ");
        if (lblSessionId   != null) lblSessionId.setText("Phiên #" + (demoItems.indexOf(item) + 1));
        // Bật nút "Tham gia đấu giá" sau khi chọn vật phẩm
        if (btnJoinAuction != null) btnJoinAuction.setDisable(false);
    }


    private void refreshStats() {
        if (lblLiveCount  != null) lblLiveCount.setText(String.valueOf(demoItems.size()));
        if (lblEndingSoon != null) lblEndingSoon.setText("2");
        if (lblLeading    != null) lblLeading.setText("0");
        if (lblTotalItems != null) lblTotalItems.setText(String.valueOf(demoItems.size()));
    }

    // ===== DATA CLASS =====

    private static class AuctionItemDemo {
        String name, seller, description, category, emoji, condition, countdown;
        double currentPrice;

        AuctionItemDemo(String name, String seller, String description,
                double currentPrice, String category,
                String emoji, String condition, String countdown) {
            this.name         = name;
            this.seller       = seller;
            this.description  = description;
            this.currentPrice = currentPrice;
            this.category     = category;
            this.emoji        = emoji;
            this.condition    = condition;
            this.countdown    = countdown;
        }
    }
}
