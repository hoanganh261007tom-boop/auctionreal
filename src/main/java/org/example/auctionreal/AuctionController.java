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
import database.dao.BidDAO;
import database.dao.AuctionDAO;

public class AuctionController {

    // --- Các phần tử UI ---
    @FXML private Label lblCountdown;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTopBidder;
    @FXML private Label lblBidCount;
    @FXML private Label lblMessage;
    @FXML private Label lblUserInfo;
    @FXML private TextField txtBidAmount;
    @FXML private TextField txtAutoBidMax; // ← field cho Auto Bid
    @FXML private ListView<String> listBidHistory;

    @FXML private Label lblItemName;
    @FXML private Label lblItemSubtitle;
    @FXML private Label lblItemEmoji;
    @FXML private Label lblItemBrand;
    @FXML private Label lblItemDescription;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMinStep;

    // Static fields: BidderDashboardController truyền dữ liệu vào đây
    public static String  selectedName        = "Rolex Submariner Date";
    public static String  selectedSubtitle    = "Ref. 126610LN • Stainless Steel • 2023";
    public static String  selectedEmoji       = "🕰";
    public static String  selectedBrand       = "ROLEX  SUBMARINER";
    public static String  selectedDescription = "Đồng hồ lặn biểu tượng của Rolex. Tình trạng: Mới 98%.";
    public static double  selectedStartPrice  = 285_000_000.0;
    public static double  selectedMinStep     = 1_000_000.0;
    public static int     selectedDuration    = 2;
    public static int     selectedAuctionId   = 1;

    private double currentPrice;
    private double MIN_STEP;
    private String topBidder = "Chưa có ai";
    private int bidCount = 0;
    private int totalSeconds;
    private Timeline countdownTimer;
    private Timeline refreshTimeline;

    // Auto Bid
    private boolean autoBidEnabled = false;
    private double  autoBidMaxPrice = 0;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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

        listBidHistory.setStyle(
                "-fx-background-color: #12122a; -fx-control-inner-background: #12122a; " +
                        "-fx-base: #12122a; -fx-border-color: transparent;"
        );

        refreshUI();
        loadBidHistoryFromDatabase();
        startRealtimeRefresh();
        startCountdown();
    }

    private void refreshUI() {
        lblCurrentPrice.setText(formatMoney(currentPrice) + " ₫");
        lblTopBidder.setText("👑 người đặt: " + topBidder);
        lblBidCount.setText(bidCount + " lượt");
    }

    private void startCountdown() {
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
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
                                "-fx-text-fill: #ff4444; -fx-font-size: 40px; " +
                                        "-fx-font-weight: bold; -fx-font-family: 'Monospaced';"
                        );
                        AuctionDAO auctionDAO = new AuctionDAO();
                        auctionDAO.closeAuction(selectedAuctionId);
                        BidDAO bidDAO = new BidDAO();
                        String winner = bidDAO.getWinner(selectedAuctionId);
                        showMessage("🏆 Người thắng: " + winner, true);
                        txtBidAmount.setDisable(true);
                        if (txtAutoBidMax != null) txtAutoBidMax.setDisable(true);
                        autoBidEnabled = false;
                    }
                })
        );
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
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

    // ─────────────────────────────
    // AUTO BID
    // ─────────────────────────────
    @FXML
    void handleEnableAutoBid(ActionEvent event) {
        if (totalSeconds <= 0) {
            showMessage("⏰ Phiên đấu giá đã kết thúc!", false);
            return;
        }

        String input = txtAutoBidMax.getText().trim().replaceAll("[.,\\s]", "");
        if (input.isEmpty()) {
            showMessage("⚠ Nhập giá tối đa để bật Auto Bid!", false);
            return;
        }

        double maxPrice;
        try {
            maxPrice = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            showMessage("❌ Giá tối đa không hợp lệ!", false);
            return;
        }

        if (maxPrice <= currentPrice) {
            showMessage("❌ Giá tối đa phải cao hơn giá hiện tại: "
                    + formatMoney(currentPrice) + " ₫!", false);
            return;
        }

        autoBidMaxPrice = maxPrice;
        autoBidEnabled  = true;
        showMessage("🤖 Auto Bid đã bật! Tối đa: "
                + formatMoney(autoBidMaxPrice) + " ₫", true);

        // Thử đặt giá ngay lập tức nếu có thể
        tryAutoBid();
    }

    /**
     * Tự động đặt giá nếu Auto Bid đang bật và còn trong hạn mức.
     */
    private void tryAutoBid() {
        if (!autoBidEnabled) return;
        if (totalSeconds <= 0) { autoBidEnabled = false; return; }

        double nextBid = currentPrice + MIN_STEP;
        if (nextBid <= autoBidMaxPrice) {
            placeBid(nextBid);
        } else {
            autoBidEnabled = false;
            showMessage("🤖 Auto Bid đã dừng: đã đạt giá tối đa "
                    + formatMoney(autoBidMaxPrice) + " ₫", false);
        }
    }

    @FXML void handleQuickBid1(ActionEvent e)  { placeBid(currentPrice + 1_000_000); }
    @FXML void handleQuickBid5(ActionEvent e)  { placeBid(currentPrice + 5_000_000); }
    @FXML void handleQuickBid10(ActionEvent e) { placeBid(currentPrice + 10_000_000); }
    @FXML void handleQuickBid50(ActionEvent e) { placeBid(currentPrice + 50_000_000); }

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
        if (user == null) {
            showMessage("❌ Bạn chưa đăng nhập!", false);
            return;
        }

        String bidderName = user.getUsername();
        int bidderId = Integer.parseInt(user.getId());

        BidDAO bidDAO = new BidDAO();
        boolean success = bidDAO.placeBid(selectedAuctionId, bidderId, amount);

        if (success) {
            currentPrice = amount;
            topBidder = bidderName;
            bidCount++;

            String historyEntry = String.format("🔺 %s → %s ₫", bidderName, formatMoney(amount));
            listBidHistory.getItems().add(0, historyEntry);
            refreshUI();

            lblCurrentPrice.setStyle(
                    "-fx-text-fill: #4af0a0; -fx-font-size: 30px; -fx-font-weight: bold;"
            );
            new Timeline(new KeyFrame(Duration.millis(800), ev ->
                    lblCurrentPrice.setStyle(
                            "-fx-text-fill: #ffffff; -fx-font-size: 30px; -fx-font-weight: bold;"
                    )
            )).play();

            showMessage("✅ Đặt giá thành công!", true);
        } else {
            showMessage("❌ Đặt giá thất bại!", false);
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (countdownTimer  != null) countdownTimer.stop();
        if (refreshTimeline != null) refreshTimeline.stop();
        autoBidEnabled = false;
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

    private void loadBidHistoryFromDatabase() {
        BidDAO bidDAO = new BidDAO();
        listBidHistory.getItems().clear();
        listBidHistory.getItems().addAll(bidDAO.getBidHistory(selectedAuctionId));
    }

    private void startRealtimeRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    BidDAO bidDAO = new BidDAO();
                    double latestBid = bidDAO.getCurrentBid(selectedAuctionId);
                    if (latestBid > currentPrice) {
                        currentPrice = latestBid;
                        refreshUI();
                        loadBidHistoryFromDatabase();
                        // Nếu Auto Bid đang bật và người khác vừa đặt cao hơn → tự động đặt lại
                        tryAutoBid();
                    }
                })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void showMessage(String msg, boolean isSuccess) {
        lblMessage.setText(msg);
        lblMessage.setStyle(isSuccess
                ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
    }

    private String formatMoney(double amount) {
        return currencyFormat.format((long) amount);
    }
}
