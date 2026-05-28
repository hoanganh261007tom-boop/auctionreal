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
import org.example.auctionreal.network.SocketClient;
import org.example.auctionreal.observer.AuctionEventManager;
import org.example.auctionreal.observer.BidObserver;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import database.dao.BidDAO;

/**
 * AuctionController – Màn hình đấu giá trực tiếp.
 *
 * Implements BidObserver để nhận thông báo qua Observer Pattern.
 * Tích hợp Anti-sniping: nếu có bid trong 30 giây cuối → gia hạn thêm 60 giây.
 */
public class AuctionController implements BidObserver {

    // ── UI elements ──
    @FXML private Label    lblCountdown, lblCurrentPrice, lblTopBidder;
    @FXML private Label    lblBidCount,  lblMessage,      lblUserInfo;
    @FXML private Label    lblItemName,  lblItemSubtitle, lblItemEmoji;
    @FXML private Label    lblItemBrand, lblItemDescription;
    @FXML private Label    lblStartPrice, lblMinStep;
    @FXML private TextField txtBidAmount, txtAutoBidMax;
    @FXML private ListView<String> listBidHistory;

    // ── Static data từ BidderDashboard ──
    public static String selectedName        = "Sản phẩm đấu giá";
    public static String selectedSubtitle    = "";
    public static String selectedEmoji       = "🏷";
    public static String selectedBrand       = "";
    public static String selectedDescription = "";
    public static double selectedStartPrice  = 0;
    public static double selectedMinStep     = 1_000_000;
    public static int    selectedDuration    = 2;
    public static int    selectedAuctionId   = 1;

    // ── State ──
    private double  currentPrice;
    private double  MIN_STEP;
    private String  topBidder   = "Chưa có ai";
    private int     bidCount    = 0;
    private volatile int totalSeconds;
    private boolean autoBidEnabled  = false;
    private double  autoBidMaxPrice = 0;

    // ── Anti-sniping (xử lý phía server, client chỉ hiển thị) ──

    // ── Timers ──
    private Timeline countdownTimer;
    private Timeline refreshTimeline;

    // ── Socket ──
    private SocketClient socketClient;
    private boolean socketConnected = false;

    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // =====================================================
    // INITIALIZE
    // =====================================================
    @FXML
    public void initialize() {
        currentPrice = selectedStartPrice;
        MIN_STEP     = selectedMinStep;
        // ★ Không tính totalSeconds local nữa → sẽ nhận từ server qua TIME_SYNC
        totalSeconds = selectedDuration * 60; // giá trị tạm, sẽ được sync ngay khi kết nối

        if (lblItemName        != null) lblItemName.setText(selectedName);
        if (lblItemSubtitle    != null) lblItemSubtitle.setText(selectedSubtitle);
        if (lblItemEmoji       != null) lblItemEmoji.setText(selectedEmoji);
        if (lblItemBrand       != null) lblItemBrand.setText(selectedBrand.toUpperCase());
        if (lblItemDescription != null) lblItemDescription.setText(selectedDescription);
        if (lblStartPrice      != null) lblStartPrice.setText(formatMoney(selectedStartPrice) + " ₫");
        if (lblMinStep         != null) lblMinStep.setText(formatMoney(selectedMinStep) + " ₫");

        User user = RegisterController.currentUser;
        lblUserInfo.setText(user != null
                ? "👤 " + user.getUsername() + "  |  " + user.getRole()
                : "👤 Khách");

        listBidHistory.setStyle("-fx-background-color: #12122a; -fx-control-inner-background: #12122a;");

        // Đăng ký Observer
        AuctionEventManager.getInstance().addObserver(this);

        refreshUI();
        loadBidHistoryFromDatabase();
        connectSocket();
        startRealtimeRefresh();
        startCountdown();
    }

    // =====================================================
    // OBSERVER PATTERN – nhận thông báo bid mới
    // =====================================================
    @Override
    public void onBidUpdated(int auctionId, double newPrice, String bidderName) {
        if (auctionId != selectedAuctionId) return;
        // Đã được xử lý qua Socket / handleBidUpdate
    }

    @Override
    public void onAuctionClosed(int auctionId, String winner) {
        if (auctionId != selectedAuctionId) return;
        showMessage("🏆 Phiên đã đóng! Người thắng: " + winner, true);
    }

    // =====================================================
    // COUNTDOWN – Hiển thị dựa trên TIME_SYNC từ server
    // =====================================================

    /**
     * Timer local chỉ dùng để hiển thị đếm ngược mượt.
     * Giá trị totalSeconds được đồng bộ từ server qua TIME_SYNC mỗi giây.
     * Server là nguồn thời gian chính thức (authoritative).
     */
    private void startCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (totalSeconds > 0) {
                // Chỉ giảm local để hiển thị mượt giữa các TIME_SYNC
                totalSeconds--;
                updateCountdownDisplay();
            } else {
                // Hiển thị hết giờ (server sẽ gửi AUCTION_CLOSED)
                lblCountdown.setText("00 : 00 : 00");
                lblCountdown.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 40px; -fx-font-weight: bold;");
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    /**
     * Cập nhật hiển thị đồng hồ đếm ngược.
     */
    private void updateCountdownDisplay() {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        lblCountdown.setText(String.format("%02d : %02d : %02d", h, m, s));
    }

    // =====================================================
    // BID HANDLING
    // =====================================================
    @FXML
    void handleBid(ActionEvent event) {
        String input = txtBidAmount.getText().trim().replaceAll("[.,\\s]", "");
        if (input.isEmpty()) {
            showMessage("⚠ Vui lòng nhập số tiền muốn đặt!", false);
            return;
        }
        try {
            placeBid(Double.parseDouble(input));
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            showMessage("❌ Số tiền không hợp lệ!", false);
        }
    }

    @FXML
    void handleEnableAutoBid(ActionEvent event) {
        if (totalSeconds <= 0) { showMessage("⏰ Phiên đã kết thúc!", false); return; }
        String input = txtAutoBidMax.getText().trim().replaceAll("[.,\\s]", "");
        if (input.isEmpty()) { showMessage("⚠ Nhập giá tối đa!", false); return; }
        try {
            double maxPrice = Double.parseDouble(input);
            if (maxPrice <= currentPrice) {
                showMessage("❌ Giá tối đa phải cao hơn " + formatMoney(currentPrice) + " ₫!", false);
                return;
            }
            autoBidMaxPrice = maxPrice;
            autoBidEnabled  = true;
            showMessage("🤖 Auto Bid bật! Tối đa: " + formatMoney(autoBidMaxPrice) + " ₫", true);
            tryAutoBid();
        } catch (NumberFormatException e) {
            showMessage("❌ Giá tối đa không hợp lệ!", false);
        }
    }

    private void tryAutoBid() {
        if (!autoBidEnabled || totalSeconds <= 0) { autoBidEnabled = false; return; }
        double nextBid = currentPrice + MIN_STEP;
        if (nextBid <= autoBidMaxPrice) {
            placeBid(nextBid);
        } else {
            autoBidEnabled = false;
            showMessage("🤖 Auto Bid dừng: đạt tối đa " + formatMoney(autoBidMaxPrice) + " ₫", false);
        }
    }

    @FXML void handleQuickBid1(ActionEvent e)  { placeBid(currentPrice + 1_000_000); }
    @FXML void handleQuickBid5(ActionEvent e)  { placeBid(currentPrice + 5_000_000); }
    @FXML void handleQuickBid10(ActionEvent e) { placeBid(currentPrice + 10_000_000); }
    @FXML void handleQuickBid50(ActionEvent e) { placeBid(currentPrice + 50_000_000); }

    private void placeBid(double amount) {
        if (totalSeconds <= 0) { showMessage("⏰ Phiên đã kết thúc!", false); return; }
        if (amount <= currentPrice) {
            showMessage("❌ Giá phải cao hơn " + formatMoney(currentPrice) + " ₫!", false); return;
        }
        if (amount < currentPrice + MIN_STEP) {
            showMessage("❌ Bước tối thiểu: " + formatMoney(MIN_STEP) + " ₫!", false); return;
        }
        User user = RegisterController.currentUser;
        if (user == null) { showMessage("❌ Chưa đăng nhập!", false); return; }

        int bidderId = user.getId();

        if (socketConnected && socketClient != null) {
            socketClient.placeBid(selectedAuctionId, bidderId, amount);
            showMessage("⏳ Đang gửi bid...", true);
            // Anti-sniping bây giờ do SERVER xử lý
        } else {
            // Fallback DAO trực tiếp
            BidDAO bidDAO = new BidDAO();
            boolean success = bidDAO.placeBid(selectedAuctionId, bidderId, amount);
            if (success) {
                currentPrice = amount;
                topBidder    = user.getUsername();
                bidCount++;
                listBidHistory.getItems().add(0,
                        "🔺 " + user.getUsername() + " → " + formatMoney(amount) + " ₫");
                refreshUI();
                flashPrice();
                showMessage("✅ Đặt giá thành công!", true);
            } else {
                showMessage("❌ Đặt giá thất bại!", false);
            }
        }
    }

    // =====================================================
    // SOCKET
    // =====================================================
    private void connectSocket() {
        socketClient = new SocketClient("localhost", 9999);
        socketClient.setOnMessageReceived(this::handleServerMessage);
        socketClient.setOnDisconnected(() -> {
            socketConnected = false;
            System.out.println("[AuctionController] Socket mất kết nối, dùng polling.");
        });
        new Thread(() -> {
            boolean ok = socketClient.connect();
            if (ok) {
                socketConnected = true;
                User user = RegisterController.currentUser;
                String name = user != null ? user.getUsername() : "Khách";
                int    uid  = user != null ? user.getId() : 0;
                socketClient.joinAuction(selectedAuctionId, uid, name);
            }
        }, "SocketConnect").start();
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split(":", 2);
        switch (parts[0]) {
            case "BID_UPDATE"     -> handleBidUpdate(message);
            case "AUCTION_CLOSED" -> handleAuctionClosed(message);
            case "TIME_SYNC"      -> handleTimeSync(message);
            case "ANTI_SNIPE"     -> handleAntiSnipe(message);
            case "USER_JOINED"    -> listBidHistory.getItems().add(0,
                    "👋 " + (parts.length > 1 ? message.split(":")[2] : "?") + " đã tham gia.");
            case "USER_LEFT"      -> listBidHistory.getItems().add(0,
                    "📤 " + (parts.length > 1 ? message.split(":")[2] : "?") + " đã rời.");
            case "ERROR"          -> showMessage("❌ " + (parts.length > 1 ? parts[1] : "Lỗi"), false);
        }
    }

    private void handleBidUpdate(String message) {
        String[] p = message.split(":");
        if (p.length < 4) return;
        try {
            int    auctionId = Integer.parseInt(p[1]);
            double newPrice  = Double.parseDouble(p[2]);
            String newTop    = p[3];
            if (auctionId != selectedAuctionId || newPrice <= currentPrice) return;

            currentPrice = newPrice;
            topBidder    = newTop;
            bidCount++;
            listBidHistory.getItems().add(0, "🔺 " + newTop + " → " + formatMoney(newPrice) + " ₫");
            refreshUI();
            flashPrice();
            tryAutoBid();
        } catch (NumberFormatException ignored) {}
    }

    /**
     * ★ Nhận TIME_SYNC từ server → cập nhật totalSeconds.
     * Format: TIME_SYNC:<auctionId>:<remainingSeconds>
     */
    private void handleTimeSync(String message) {
        String[] p = message.split(":");
        if (p.length < 3) return;
        try {
            int auctionId = Integer.parseInt(p[1]);
            if (auctionId != selectedAuctionId) return;
            int serverRemaining = Integer.parseInt(p[2]);

            // Đồng bộ thời gian từ server (nguồn chính thức)
            totalSeconds = serverRemaining;
            updateCountdownDisplay();

            if (serverRemaining <= 0) {
                lblCountdown.setText("00 : 00 : 00");
                lblCountdown.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 40px; -fx-font-weight: bold;");
            }
        } catch (NumberFormatException ignored) {}
    }

    /**
     * ★ Nhận ANTI_SNIPE từ server → hiển thị thông báo gia hạn.
     * Format: ANTI_SNIPE:<auctionId>:<extraSeconds>
     */
    private void handleAntiSnipe(String message) {
        String[] p = message.split(":");
        if (p.length < 3) return;
        try {
            int auctionId = Integer.parseInt(p[1]);
            if (auctionId != selectedAuctionId) return;
            int extraSeconds = Integer.parseInt(p[2]);
            showMessage("⏱ Anti-sniping! Phiên được gia hạn thêm " + extraSeconds + " giây!", true);
            System.out.println("[AuctionController] Anti-sniping từ server: +" + extraSeconds + "s");
        } catch (NumberFormatException ignored) {}
    }

    private void handleAuctionClosed(String message) {
        String[] p = message.split(":");
        if (p.length < 3) return;
        if (Integer.parseInt(p[1]) != selectedAuctionId) return;
        totalSeconds = 0;
        lblCountdown.setText("00 : 00 : 00");
        lblCountdown.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 40px; -fx-font-weight: bold;");
        txtBidAmount.setDisable(true);
        if (txtAutoBidMax != null) txtAutoBidMax.setDisable(true);
        autoBidEnabled = false;
        showMessage("🏆 Phiên đóng! Người thắng: " + p[2], true);

        // Thông báo Observer
        AuctionEventManager.getInstance()
                .notifyAuctionClosed(selectedAuctionId, p[2]);
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private void refreshUI() {
        lblCurrentPrice.setText(formatMoney(currentPrice) + " ₫");
        lblTopBidder.setText("👑 " + topBidder);
        lblBidCount.setText(bidCount + " lượt");
    }

    private void flashPrice() {
        lblCurrentPrice.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 30px; -fx-font-weight: bold;");
        new Timeline(new KeyFrame(Duration.millis(800), ev ->
                lblCurrentPrice.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 30px; -fx-font-weight: bold;")
        )).play();
    }

    private void loadBidHistoryFromDatabase() {
        BidDAO bidDAO = new BidDAO();
        listBidHistory.getItems().clear();
        listBidHistory.getItems().addAll(bidDAO.getBidHistory(selectedAuctionId));
    }

    private void startRealtimeRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            BidDAO bidDAO = new BidDAO();
            double latest = bidDAO.getCurrentBid(selectedAuctionId);
            if (latest > currentPrice) {
                currentPrice = latest;
                refreshUI();
                loadBidHistoryFromDatabase();
                tryAutoBid();
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void showMessage(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle(ok
                ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private String formatMoney(double amount) {
        return fmt.format((long) amount);
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (countdownTimer  != null) countdownTimer.stop();
        if (refreshTimeline != null) refreshTimeline.stop();
        autoBidEnabled = false;
        AuctionEventManager.getInstance().removeObserver(this);
        if (socketClient != null) { socketClient.disconnect(); socketConnected = false; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 600, 400));
            stage.setMinWidth(600); stage.setMinHeight(400);
            stage.setTitle("Lựa chọn vai trò");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
