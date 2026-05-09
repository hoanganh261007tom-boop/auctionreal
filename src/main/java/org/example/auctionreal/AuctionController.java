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

    // =========================================================
    // Static fields: BidderDashboardController truyền dữ liệu
    // vật phẩm được chọn vào đây trước khi load auction.fxml
    // =========================================================
    public static String  selectedName        = "Rolex Submariner Date";
    public static String  selectedSubtitle     = "Ref. 126610LN \u2022 Stainless Steel \u2022 2023";
    public static String  selectedEmoji        = "\uD83D\uDD70";  // 🕰
    public static String  selectedBrand        = "ROLEX  SUBMARINER";
    public static String  selectedDescription  = "Đồng hồ lặn biểu tượng của Rolex. Tình trạng: Mới 98%.";
    public static double  selectedStartPrice   = 285_000_000.0;
    public static double  selectedMinStep      = 1_000_000.0;
    public static int     selectedDuration     = 2; // phút

    // --- Dữ liệu nghiệp vụ (lấy từ static fields được truyền vào) ---
    private double currentPrice;          // Giá khởi điểm ban đầu
    private double MIN_STEP;              // Bước giá tối thiểu
    private String topBidder = "Chưa có ai";
    private int bidCount = 0;

    // --- Đếm ngược: lấy từ selectedDuration ---
    private int totalSeconds; // sẽ được set trong initialize()
    private Timeline countdownTimer;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // --- Các phần tử UI ---
    @FXML private Label lblCountdown;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTopBidder;
    @FXML private Label lblBidCount;
    @FXML private Label lblMessage;
    @FXML private Label lblUserInfo;
    @FXML private TextField txtBidAmount;
    @FXML private ListView<String> listBidHistory;

    // --- Labels hiển thị thông tin vật phẩm (được set fx:id trong FXML) ---
    @FXML private Label lblItemName;
    @FXML private Label lblItemSubtitle;
    @FXML private Label lblItemEmoji;
    @FXML private Label lblItemBrand;
    @FXML private Label lblItemDescription;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMinStep;

    // ===== TIỆN ÍCH PRIVATE =====
    // (khai báo trước initialize() để dễ hiểu khi đọc từ trên xuống)

    /** Định dạng số tiền theo locale Việt Nam. */
    private String formatMoney(double amount) {
        return currencyFormat.format((long) amount);
    }

    /** Hiển thị thông báo kết quả thành công / thất bại. */
    private void showMessage(String msg, boolean isSuccess) {
        lblMessage.setText(msg);
        if (isSuccess) {
            lblMessage.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            lblMessage.setStyle("-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
    }

    /** Cập nhật toàn bộ UI theo dữ liệu hiện tại. */
    private void refreshUI() {
        lblCurrentPrice.setText(formatMoney(currentPrice) + " ₫");
        lblTopBidder.setText("👑 người đặt: " + topBidder);
        lblBidCount.setText(bidCount + " lượt");
    }

    /** Bắt đầu đồng hồ đếm ngược. */
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
                lblCountdown.setStyle(
                        "-fx-text-fill: #ff4444; -fx-font-size: 40px; -fx-font-weight: bold; -fx-font-family: 'Monospaced';"
                );
                showMessage("⏰ Phiên đấu giá đã kết thúc! Người thắng: " + topBidder, false);
                // Vô hiệu hoá nút đặt giá
                txtBidAmount.setDisable(true);
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    /** Logic xử lý một lượt đặt giá. */
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

        // Lấy tên người đặt từ user hiện tại
        User user = RegisterController.currentUser;
        String bidderName = (user != null) ? user.getUsername() : "Ẩn danh";

        // Cập nhật dữ liệu
        currentPrice = amount;
        topBidder = bidderName;
        bidCount++;

        // Thêm vào lịch sử
        String historyEntry = String.format("🔺 %s → %s ₫", bidderName, formatMoney(amount));
        listBidHistory.getItems().add(0, historyEntry); // Thêm lên đầu danh sách

        // Style dòng mới nhất
        listBidHistory.getStyleClass().add("bid-list");

        refreshUI();

        // Hiệu ứng làm nổi bật giá mới
        lblCurrentPrice.setStyle(
                "-fx-text-fill: #4af0a0; -fx-font-size: 30px; -fx-font-weight: bold;"
        );
        new Timeline(new KeyFrame(Duration.millis(800), ev ->
                lblCurrentPrice.setStyle(
                        "-fx-text-fill: #ffffff; -fx-font-size: 30px; -fx-font-weight: bold;"
                )
        )).play();

        showMessage("✅ Đặt giá thành công! Bạn đang dẫn đầu với " + formatMoney(amount) + " ₫", true);
    }

    // ===== KHỞI TẠO =====

    @FXML
    public void initialize() {
        // ---- Áp dụng dữ liệu vật phẩm được chọn từ BidderDashboard ----
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

        // Hiển thị thông tin người dùng hiện tại
        User user = RegisterController.currentUser;
        if (user != null) {
            lblUserInfo.setText("👤 " + user.getUsername() + "  |  " + user.getRole().toUpperCase());
        } else {
            lblUserInfo.setText("👤 Khách");
        }

        // Style cho ListView
        listBidHistory.setStyle(
                "-fx-background-color: #12122a; -fx-control-inner-background: #12122a; " +
                        "-fx-base: #12122a; -fx-border-color: transparent;"
        );

        refreshUI();
        startCountdown();
    }

    // ===== XỬ LÝ SỰ KIỆN ĐẶT GIÁ =====

    @FXML
    void handleBid(ActionEvent event) {
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showMessage("⚠ Vui lòng nhập số tiền muốn đặt!", false);
            return;
        }

        // Loại bỏ dấu chấm phân cách nếu người dùng tự nhập
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

    /** Quay lại trang chọn vai trò. */
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
}
