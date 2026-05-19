package org.example.auctionreal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import user.User;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class AuctionController {

    // ===== Dữ liệu tĩnh truyền từ BidderDashboard =====
    public static String  selectedName        = "Rolex Submariner Date";
    public static String  selectedSubtitle    = "Ref. 126610LN \u2022 Stainless Steel \u2022 2023";
    public static String  selectedEmoji       = "\uD83D\uDD70";
    public static String  selectedBrand       = "ROLEX  SUBMARINER";
    public static String  selectedDescription  = "Đồng hồ lặn biểu tượng của Rolex. Tình trạng: Mới 98%.";
    public static double  selectedStartPrice   = 285_000_000.0;
    public static double  selectedMinStep      = 1_000_000.0;
    public static int     selectedDuration     = 2; // phút

    // ===== Biến trạng thái runtime =====
    private double MIN_STEP;
    private double currentPrice;
    private String topBidder = "Chưa có ai";
    private int    bidCount  = 0;
    private int    totalSeconds;
    private Timeline countdownTimer;

    // ===== AUTO BID =====
    private boolean autoBidEnabled = false;
    private double autoBidMaxAmount = 0;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ===== FXML Fields =====
    @FXML private Label lblCountdown;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTopBidder;
    @FXML private Label lblBidCount;
    @FXML private Label lblMessage;
    @FXML private Label lblUserInfo;
    @FXML private TextField txtBidAmount;
    @FXML private TextField txtAutoBidMax;
    @FXML private ListView<String> listBidHistory;

    @FXML private Label lblItemName;
    @FXML private Label lblItemSubtitle;
    @FXML private Label lblItemEmoji;
    @FXML private Label lblItemBrand;
    @FXML private Label lblItemDescription;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMinStep;

    // ===== Khởi tạo =====
    @FXML
    public void initialize() {
        MIN_STEP     = selectedMinStep;
        currentPrice = selectedStartPrice;
        totalSeconds = selectedDuration * 60; // phút → giây

        if (lblItemName        != null) lblItemName.setText(selectedName);
        if (lblItemSubtitle    != null) lblItemSubtitle.setText(selectedSubtitle);
        if (lblItemEmoji       != null) lblItemEmoji.setText(selectedEmoji);
        if (lblItemBrand       != null) lblItemBrand.setText(selectedBrand);
        if (lblItemDescription != null) lblItemDescription.setText(selectedDescription);
        if (lblStartPrice      != null) lblStartPrice.setText(formatMoney(selectedStartPrice) + " ₫");
        if (lblMinStep         != null) lblMinStep.setText("+" + formatMoney(MIN_STEP) + " ₫");

        User user = RegisterController.currentUser;
        if (lblUserInfo != null) {
            if (user instanceof user.Bidder bidder) {
                lblUserInfo.setText(
                        "👤 " + user.getUsername() + "  |  💰 " + formatMoney(bidder.getBalance()) + " ₫");
            } else {
                lblUserInfo.setText(user != null ? "👤 " + user.getUsername() : "👤 Khách");
            }
        }

        refreshUI();
        startCountdown();
    }
    private void showMessage(String msg, boolean isSuccess) {
        lblMessage.setText(msg);
        if (isSuccess) {
            lblMessage.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            lblMessage.setStyle("-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
    }

    // ===== Xử lý sự kiện =====
    @FXML
    void handleBid(ActionEvent event) {
        String raw = txtBidAmount.getText().trim().replaceAll("[.,\\s]", "");
        if (raw.isEmpty()) {
            showMessage("⚠ Vui lòng nhập số tiền đặt giá!", false);
            return;
        }
        try {
            double amount = Double.parseDouble(raw);
            placeBid(amount);
        } catch (NumberFormatException e) {
            showMessage("❌ Số tiền không hợp lệ!", false);
        }
    }

    // Đặt giá nhanh lấy giá hiện tại cộng thêm số tiền tương ứng
    @FXML void handleQuickBid1(ActionEvent e)  { placeBid(currentPrice + 1_000_000); }
    @FXML void handleQuickBid5(ActionEvent e)  { placeBid(currentPrice + 5_000_000); }
    @FXML void handleQuickBid10(ActionEvent e) { placeBid(currentPrice + 10_000_000); }
    @FXML void handleQuickBid50(ActionEvent e) { placeBid(currentPrice + 50_000_000); }

    @FXML
    void handleBack(ActionEvent event) {
        if (countdownTimer != null) countdownTimer.stop();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 600, 400));
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            stage.setTitle("Lựa chọn vai trò");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleEnableAutoBid(ActionEvent event) {
        String input = txtAutoBidMax.getText().trim().replaceAll("[.,\\s]", "");
        if (input.isEmpty()) {
            showMessage("⚠ Vui lòng nhập giá tối đa auto bid!", false);
            return;
        }
        try {
            double maxAmount = Double.parseDouble(input);
            if (maxAmount <= currentPrice) {
                showMessage("❌ Giá auto bid phải lớn hơn giá hiện tại!", false);
                return;
            }
            autoBidEnabled = true;
            autoBidMaxAmount = maxAmount;
            showMessage("🤖 Auto bidding đã bật tới " + formatMoney(maxAmount) + " ₫", true);
        } catch (NumberFormatException e) {
            showMessage("❌ Giá auto bid không hợp lệ!", false);
        }
    }

    // ===== Tiện ích private =====
    private void refreshUI() {
        if (lblCurrentPrice != null) lblCurrentPrice.setText(formatMoney(currentPrice) + " ₫");
        if (lblTopBidder    != null) lblTopBidder.setText("👑 người đặt: " + topBidder);
        if (lblBidCount     != null) lblBidCount.setText(bidCount + " lượt");
    }

    private void startCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (totalSeconds > 0) {
                totalSeconds--;
                int h = totalSeconds / 3600;
                int m = (totalSeconds % 3600) / 60;
                int s = totalSeconds % 60;
                if (lblCountdown != null)
                    lblCountdown.setText(String.format("%02d : %02d : %02d", h, m, s));
            } else {
                countdownTimer.stop();
                if (lblCountdown != null) lblCountdown.setText("00 : 00 : 00");
                showMessage("⏰ Phiên đấu giá đã kết thúc! Người thắng: " + topBidder, false);
                if (txtBidAmount != null) txtBidAmount.setDisable(true);
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void placeBid(double amount) {
        if (totalSeconds <= 0) {
            showMessage("⏰ Phiên đấu giá đã kết thúc!", false);
            return;
        }
        if (amount <= currentPrice) {
            showMessage("❌ Giá đặt phải cao hơn " + formatMoney(currentPrice) + " ₫!", false);
            return;
        }
        if (amount < currentPrice + MIN_STEP) {
            showMessage("❌ Bước giá tối thiểu là " + formatMoney(MIN_STEP) + " ₫!", false);
            return;
        }

        User user = RegisterController.currentUser;
        String bidderName = (user != null) ? user.getUsername() : "Ẩn danh";

        if (user instanceof user.Bidder bidder) {
            if (amount > bidder.getBalance()) {
                showMessage("💸 Không đủ số dư! Bạn chỉ còn " + formatMoney(bidder.getBalance()) + " ₫", false);
                return;
            }
        }

        currentPrice = amount;
        topBidder    = bidderName;
        bidCount++;

        if (user instanceof user.Bidder bidder) {
            bidder.setBalance(bidder.getBalance() - amount);
            if (lblUserInfo != null) {
                lblUserInfo.setText("👤 " + user.getUsername() + "  |  💰 " + formatMoney(bidder.getBalance()) + " ₫");
            }
        }

        String historyEntry = String.format("🔺 %s → %s ₫", bidderName, formatMoney(amount));
        if (listBidHistory != null) {
            listBidHistory.getItems().add(0, historyEntry);
        }

        if (txtBidAmount != null) txtBidAmount.clear();
        refreshUI();
        showMessage("✅ Đặt giá thành công: " + formatMoney(amount) + " ₫", true);

        // ===== AUTO BIDDING LOGIC =====
        if (autoBidEnabled && user != null && !bidderName.equals(user.getUsername())) {
            double nextBid = amount + MIN_STEP;
            if (nextBid <= autoBidMaxAmount) {
                Timeline autoBidDelay = new Timeline(
                        new KeyFrame(Duration.seconds(1), ev -> placeBid(nextBid))
                );
                autoBidDelay.play();
                showMessage("🤖 Hệ thống đang tự động nâng giá...", true);
            } else {
                showMessage("⚠ Auto bid đã đạt giới hạn tối đa!", false);
                autoBidEnabled = false;
            }
        }
    }

    /** Được gọi bởi AuctionSocketClient khi nhận bid mới từ server (Tính năng của master) */
    public void applyNewBidToUI(double amount, String bidderName) {
        currentPrice = amount;
        topBidder    = bidderName;
        bidCount++;
        String historyEntry = String.format("🔺 %s → %s ₫", bidderName, formatMoney(amount));
        if (listBidHistory != null) {
            listBidHistory.getItems().add(0, historyEntry);
        }
        refreshUI();
        showMessage("🌐 Giá mới từ " + bidderName + ": " + formatMoney(amount) + " ₫", true);

        // Kích hoạt ngược lại AutoBid nếu người vừa đặt online không phải là mình
        User user = RegisterController.currentUser;
        if (autoBidEnabled && user != null && !bidderName.equals(user.getUsername())) {
            double nextBid = amount + MIN_STEP;
            if (nextBid <= autoBidMaxAmount) {
                Timeline autoBidDelay = new Timeline(
                        new KeyFrame(Duration.seconds(1), ev -> placeBid(nextBid))
                );
                autoBidDelay.play();
            }
        }
    }

    private String formatMoney(double amount) {
        return currencyFormat.format((long) amount);
    }
}