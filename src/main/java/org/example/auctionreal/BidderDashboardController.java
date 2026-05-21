package org.example.auctionreal;

import database.dao.ItemDAO;
import database.dao.ItemDAO.AuctionItemInfo;
import database.dao.WatchlistDAO;
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

public class BidderDashboardController {

    // ===== UI Elements =====
    @FXML private Label lblUserInfo;
    @FXML private Label lblBalance;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterCategory;
    @FXML private ComboBox<String> cmbFilterPrice;
    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private Label lblLiveCount;
    @FXML private Label lblEndingSoon;
    @FXML private Label lblLeading;
    @FXML private Label lblTotalItems;
    @FXML private ListView<String> listAuctionItems;
    @FXML private ComboBox<String> cmbSort;
    @FXML private Label lblPage;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblSessionId;
    @FXML private Label lblItemName;
    @FXML private Label lblSellerName;
    @FXML private Label lblItemEmoji;
    @FXML private Label lblItemCategory;
    @FXML private Label lblDescription;
    @FXML private HBox hboxTags;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTopBidder;
    @FXML private Label lblCountdown;
    @FXML private Button btnJoinAuction;
    @FXML private Button btnWatchlist;

    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /** Danh sách item từ DB (hoặc demo nếu DB trống). */
    private final List<AuctionItemInfo> auctionItems = new ArrayList<>();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
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

        cmbFilterCategory.getItems().addAll("Tất cả","Đồng hồ","Điện tử","Trang sức","Xe cộ","Nghệ thuật","Thời trang","Khác");
        cmbFilterCategory.setValue("Tất cả");
        cmbFilterPrice.getItems().addAll("Tất cả","Dưới 10 triệu","10 - 100 triệu","100 - 500 triệu","Trên 500 triệu");
        cmbFilterPrice.setValue("Tất cả");
        cmbFilterStatus.getItems().addAll("Tất cả","Đang diễn ra","Sắp kết thúc (30 phút)","Sắp bắt đầu");
        cmbFilterStatus.setValue("Tất cả");
        cmbSort.getItems().addAll("Mới nhất","Giá thấp → cao","Giá cao → thấp","Kết thúc sớm nhất");
        cmbSort.setValue("Mới nhất");

        loadItemsFromDatabase();
        refreshItemList();
        refreshStats();
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().trim().toLowerCase();
        listAuctionItems.getItems().clear();
        for (AuctionItemInfo item : auctionItems) {
            if (keyword.isEmpty() || item.name.toLowerCase().contains(keyword)) {
                listAuctionItems.getItems().add(formatItemForList(item));
            }
        }
        lblPage.setText("Trang 1 / 1");
    }

    @FXML
    void handleItemSelected(javafx.scene.input.MouseEvent event) {
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= auctionItems.size()) return;
        updateDetailPanel(auctionItems.get(idx));
    }

    @FXML
    void handleJoinAuction(ActionEvent event) {
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= auctionItems.size()) return;

        AuctionItemInfo item = auctionItems.get(idx);

        // ---- Truyền đầy đủ dữ liệu vào AuctionController ----
        AuctionController.selectedName        = item.name;
        AuctionController.selectedSubtitle    = item.sellerName + "  •  " + item.status;
        AuctionController.selectedEmoji       = "🏷";
        AuctionController.selectedBrand       = item.name.toUpperCase();
        AuctionController.selectedDescription = item.description != null ? item.description : "(Không có mô tả)";
        AuctionController.selectedStartPrice  = item.currentPrice > 0 ? item.currentPrice : item.startPrice;
        AuctionController.selectedMinStep     = item.minStep > 0 ? item.minStep : 1_000_000.0;
        AuctionController.selectedDuration    = 2; // mặc định, có thể mở rộng
        AuctionController.selectedAuctionId   = item.auctionId; // ← auction_id THẬT từ DB

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
        User user = RegisterController.currentUser;
        if (user == null) return;

        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= auctionItems.size()) return;

        int auctionId = auctionItems.get(idx).auctionId;
        if (auctionId == -1) {
            btnWatchlist.setText("❌ Chưa có phiên đấu giá");
            return;
        }

        int userId = Integer.parseInt(user.getId());
        WatchlistDAO watchlistDAO = new WatchlistDAO();
        boolean success = watchlistDAO.addToWatchlist(userId, auctionId);
        btnWatchlist.setText(success ? "✅ Đã thêm theo dõi" : "❌ Lỗi watchlist");
    }

    @FXML
    void handleMyBids(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Lịch sử đặt giá");
        info.setHeaderText("📋 Lịch sử đặt giá của bạn");
        info.setContentText("Tính năng này sẽ hiển thị toàn bộ lịch sử đặt giá của bạn.");
        info.showAndWait();
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) { currentPage--; refreshItemList(); }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        int totalPages = Math.max(1, (int) Math.ceil((double) auctionItems.size() / PAGE_SIZE));
        if (currentPage < totalPages) { currentPage++; refreshItemList(); }
    }

    @FXML
    void handleFilter(ActionEvent event) { handleSearch(event); }

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

    // ===== PRIVATE METHODS =====

    private void loadItemsFromDatabase() {
        auctionItems.clear();
        ItemDAO itemDAO = new ItemDAO();
        List<AuctionItemInfo> dbItems = itemDAO.getAllAuctionItems();

        if (!dbItems.isEmpty()) {
            auctionItems.addAll(dbItems);
        } else {
            // Fallback demo nếu DB trống
            System.out.println("[BidderDashboard] DB trống, dùng demo data.");
            loadDemoItems();
        }
    }

    private void loadDemoItems() {
        AuctionItemInfo d1 = new AuctionItemInfo();
        d1.auctionId = -1; d1.name = "Rolex Submariner 2023"; d1.sellerName = "Nguyen Hoang Anh";
        d1.description = "Đồng hồ lặn biểu tượng, bezel ceramic đen, tình trạng mới 98%.";
        d1.startPrice = 285_000_000; d1.currentPrice = 285_000_000; d1.minStep = 1_000_000; d1.status = "OPEN";
        auctionItems.add(d1);

        AuctionItemInfo d2 = new AuctionItemInfo();
        d2.auctionId = -1; d2.name = "iPhone 15 Pro Max 256GB"; d2.sellerName = "Le Dinh Bach";
        d2.description = "Máy nguyên seal, màu titan tự nhiên, bảo hành Apple 12 tháng.";
        d2.startPrice = 35_000_000; d2.currentPrice = 35_000_000; d2.minStep = 500_000; d2.status = "OPEN";
        auctionItems.add(d2);
    }

    private void refreshItemList() {
        listAuctionItems.getItems().clear();
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, auctionItems.size());
        for (int i = start; i < end; i++) {
            listAuctionItems.getItems().add(formatItemForList(auctionItems.get(i)));
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) auctionItems.size() / PAGE_SIZE));
        lblPage.setText("Trang " + currentPage + " / " + totalPages);
    }

    private void refreshStats() {
        lblTotalItems.setText(String.valueOf(auctionItems.size()));
        lblLiveCount.setText(String.valueOf(auctionItems.size()));
        lblEndingSoon.setText("0");
        lblLeading.setText("0");
    }

    private String formatItemForList(AuctionItemInfo item) {
        double displayPrice = item.currentPrice > 0 ? item.currentPrice : item.startPrice;
        return String.format("🏷 %s  |  %s ₫  |  👤 %s  |  %s",
                item.name,
                currencyFormat.format((long) displayPrice),
                item.sellerName,
                item.status != null ? item.status : "OPEN");
    }

    private void updateDetailPanel(AuctionItemInfo item) {
        lblItemName.setText(item.name);
        lblSellerName.setText("Người bán: " + item.sellerName);
        lblItemEmoji.setText("🏷");
        lblItemCategory.setText(item.status != null ? item.status : "OPEN");
        lblDescription.setText(item.description != null && !item.description.isBlank()
                ? item.description : "(Không có mô tả)");

        double displayPrice = item.currentPrice > 0 ? item.currentPrice : item.startPrice;
        lblCurrentPrice.setText(currencyFormat.format((long) displayPrice) + " ₫");
        lblTopBidder.setText("👑 Chưa có ai dẫn đầu");
        lblCountdown.setText("--:--:--");
        lblSessionId.setText(item.auctionId > 0 ? "Phiên #" + item.auctionId : "Chưa có phiên");

        hboxTags.getChildren().clear();
        Label tagStatus = new Label(item.status != null ? item.status : "OPEN");
        tagStatus.setStyle("-fx-text-fill: #4af0a0; -fx-background-color: #0a2a1a; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        Label tagStep = new Label("Bước: " + currencyFormat.format((long) item.minStep) + " ₫");
        tagStep.setStyle("-fx-text-fill: #4ab0f0; -fx-background-color: #0a1a2a; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        hboxTags.getChildren().addAll(tagStatus, tagStep);

        btnJoinAuction.setDisable(item.auctionId == -1);
        btnWatchlist.setText("🔖  Thêm vào danh sách theo dõi");
        btnWatchlist.setStyle("-fx-background-color: #1e1e3a; -fx-text-fill: #aaaacc; -fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}

