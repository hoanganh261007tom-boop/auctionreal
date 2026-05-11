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

    // ===== Các phần tử UI (phải khớp fx:id trong bidder-dashboard.fxml) =====

    // Thanh trên

    // Thanh tìm kiếm & bộ lọc

    // Thống kê nhanh

    // Danh sách vật phẩm

    // Panel chi tiết





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

        refreshItemList();
        refreshStats();
    }

    // ===== XỬ LÝ SỰ KIỆN =====

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

    @FXML
    void handleWatchlist(ActionEvent event) {
        int idx = listAuctionItems.getSelectionModel().getSelectedIndex();
        if (idx < 0)
            return;
        // TODO: Lưu danh sách theo dõi vào database
        btnWatchlist.setText("✅  Đã thêm vào danh sách theo dõi");
        btnWatchlist.setStyle(
                "-fx-background-color: #0a2a1a; -fx-text-fill: #4af0a0; -fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;");
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
