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

    public static String  selectedName        = "Rolex Submariner Date";
    public static String  selectedSubtitle     = "Ref. 126610LN \u2022 Stainless Steel \u2022 2023";
    public static String  selectedEmoji        = "\uD83D\uDD70";
    public static String  selectedBrand        = "ROLEX  SUBMARINER";
    public static String  selectedDescription  = "Đồng hồ lặn biểu tượng của Rolex. Tình trạng: Mới 98%.";
    public static double  selectedStartPrice   = 285_000_000.0;
    public static double  selectedMinStep      = 1_000_000.0;
    public static int     selectedDuration     = 2;

    private double currentPrice;
    private double MIN_STEP;
    private String topBidder = "Chưa có ai";
    private int bidCount = 0;

    private int totalSeconds;
    private Timeline countdownTimer;

    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // --- Đối tượng quản lý kết nối Socket ---
    private AuctionSocketClient socketClient;

    @FXML private Label lblCountdown;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTopBidder;
    @FXML private Label lblBidCount;
    @FXML private Label lblMessage;
    @FXML private Label lblUserInfo;
    @FXML private TextField txtBidAmount;
    @FXML private ListView<String> listBidHistory;

    @FXML private Label lblItemName;
    @FXML private Label lblItemSubtitle;
    @FXML private Label lblItemEmoji;
    @FXML private Label lblItemBrand;
    @FXML private Label lblItemDescription;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMinStep;

    private String formatMoney(double amount) {
        return currencyFormat.format((long) amount);
    }

    private void showMessage(String msg, boolean isSuccess) {
        lblMessage.setText(msg);
        if (isSuccess) {
            lblMessage.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            lblMessage.setStyle("-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
    }

    private void refreshUI() {
        lblCurrentPrice.setText(formatMoney(currentPrice) + " ₫");
        lblTopBidder.setText("👑 người đặt: " + topBidder);
        lblBidCount.setText(bidCount + " lượt");
    }

    private void startCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (totalSeconds > 0) {
                totalSeconds--;
                int h = totalSeconds / 3600;
                int m = (totalSeconds % 3600) / 60;
                int s = totalSeconds % 60;
                lblCountdown.setText(String.format("%02d : %02d : %02d", h, m, s));
            } else {
                countdownTimer.stop();
                lblCountdown.setText("00 : 00 : 00");
                lblCountdown.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 40px; -fx-font-weight: bold; -fx-font-family: 'Monospaced';");
                showMessage("⏰ Phiên đấu giá đã kết thúc! Người thắng: " + topBidder, false);
                txtBidAmount.setDisable(true);
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    /** Logic KIỂM TRA và GỬI giá lên Server (Không cập nhật UI ở đây nữa) */
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

        // Thay vì cập nhật UI, ta GỬI dữ liệu qua Socket
        if (socketClient != null) {
            socketClient.sendBid(bidderName, amount);
            showMessage("⏳ Đang gửi yêu cầu lên hệ thống...", true);
        } else {
            showMessage("❌ Mất kết nối tới máy chủ!", false);
        }
    }

    /** Hàm này được Server gọi (thông qua SocketClient) để cập nhật giao diện */
    public void applyNewBidToUI(double amount, String bidderName) {
        currentPrice = amount;
        topBidder = bidderName;
        bidCount++;

        String historyEntry = String.format("🔺 %s → %s ₫", bidderName, formatMoney(amount));
        listBidHistory.getItems().add(0, historyEntry);
        listBidHistory.getStyleClass().add("bid-list");

        refreshUI();

        lblCurrentPrice.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 30px; -fx-font-weight: bold;");
        new Timeline(new KeyFrame(Duration.millis(800), ev ->
                lblCurrentPrice.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 30px; -fx-font-weight: bold;")
        )).play();

        // Kiểm tra xem có phải user hiện tại vừa đặt thành công không
        User user = RegisterController.currentUser;
        if (user != null && user.getUsername().equals(bidderName)) {
            showMessage("✅ Đặt giá thành công! Bạn đang dẫn đầu.", true);
        } else {
            showMessage("🔥 " + bidderName + " vừa nâng giá!", false);
        }
    }

    @FXML
    public void initialize() {
        currentPrice = selectedStartPrice;
        MIN_STEP     = selectedMinStep;
        totalSeconds = selectedDuration * 60;

        if (lblItemName        != null) lblItemName.setText(selectedName);
        if (lblItemSubtitle    != null) lblItemSubtitle.setText(selectedSubtitle);
        if (lblItemEmoji       != null) lblItemEmoji.setText(selectedEmoji);
        if (lblItemBrand       != null) lblItemBrand.setText(selectedBrand.toUpperCase());
        if (lblItemDescription != null) lblItemDescription.setText(selectedDescription);
        if (lblStartPrice      != null) lblStartPrice.setText(formatMoney(selectedStartPrice) + " ₫");
        if (lblMinStep         != null) lblMinStep.setText(formatMoney(selectedMinStep) + " ₫");

        User user = RegisterController.currentUser;
        if (user != null) {
            lblUserInfo.setText("👤 " + user.getUsername() + "  |  " + user.getRole().toUpperCase());
        } else {
            lblUserInfo.setText("👤 Khách");
        }

        listBidHistory.setStyle("-fx-background-color: #12122a; -fx-control-inner-background: #12122a; -fx-base: #12122a; -fx-border-color: transparent;");

        refreshUI();
        startCountdown();

        // Khởi tạo Socket và kết nối tới Server (Giả sử Server chạy ở localhost port 1234)
        socketClient = new AuctionSocketClient(this);
        socketClient.connectToServer("localhost", 1234);
    }

    @FXML
    void handleBid(ActionEvent event) {
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showMessage("⚠ Vui lòng nhập số tiền muốn đặt!", false);
            return;
        }

        input = input.replaceAll("[.,\\s]", "");
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            showMessage("❌ Số tiền không hợp lệ! Chỉ nhập chữ số.", false);
            return;
        }

        placeBid(bidAmount);
        txtBidAmount.clear();
    }

    @FXML void handleQuickBid1(ActionEvent e)  { placeBid(currentPrice + 1_000_000); }
    @FXML void handleQuickBid5(ActionEvent e)  { placeBid(currentPrice + 5_000_000); }
    @FXML void handleQuickBid10(ActionEvent e) { placeBid(currentPrice + 10_000_000); }
    @FXML void handleQuickBid50(ActionEvent e) { placeBid(currentPrice + 50_000_000); }

    @FXML
    void handleBack(ActionEvent event) {
        if (countdownTimer != null) countdownTimer.stop();

        // Cần đóng kết nối Socket khi thoát màn hình để không tốn tài nguyên
        if (socketClient != null) {
            socketClient.disconnect();
        }

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
}