package org.example.auctionreal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import user.User;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import database.dao.ItemDAO;
import database.dao.AuctionDAO;

import java.sql.Timestamp;

/**
 * SellerDashboardController: Xử lý màn hình dành cho Người Bán (Seller).
 * Cho phép Seller đăng vật phẩm muốn bán đấu giá và xem danh sách vật phẩm đã đăng.
 */
public class SellerDashboardController {

    // ===== Các phần tử UI (phải khớp fx:id trong seller-dashboard.fxml) =====

    // Thanh trên
    @FXML private Label lblUserInfo;

    // Form đăng vật phẩm
    @FXML private TextField txtItemName;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtMinStep;
    @FXML private TextField txtDurationHours;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private RadioButton rbNew;
    @FXML private RadioButton rbLikeNew;
    @FXML private RadioButton rbUsed;
    @FXML private Label lblMessage;

    // Cột phải – danh sách vật phẩm
    @FXML private Label lblItemCount;
    @FXML private Label lblTotalItems;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblSoldItems;
    @FXML private TextField txtSearch;
    @FXML private ListView<String> listMyItems;

    // ===== Dữ liệu nội bộ =====
    private final ToggleGroup conditionGroup = new ToggleGroup();
    private int totalItems   = 0;
    private int activeCount  = 0;
    private int soldCount    = 0;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ===== Khởi tạo =====

    @FXML
    public void initialize() {
        // Hiển thị thông tin người dùng
        User user = RegisterController.currentUser;
        if (user != null) {
            lblUserInfo.setText("🏪 " + user.getUsername() + "  |  SELLER");
        } else {
            lblUserInfo.setText("🏪 Người bán");
        }

        // Nhóm RadioButton
        rbNew.setToggleGroup(conditionGroup);
        rbLikeNew.setToggleGroup(conditionGroup);
        rbUsed.setToggleGroup(conditionGroup);
        rbLikeNew.setSelected(true); // mặc định

        // Đổ dữ liệu danh mục
        cmbCategory.getItems().addAll(
                "Đồng hồ cao cấp",
                "Điện tử & Công nghệ",
                "Trang sức & Đá quý",
                "Xe cộ",
                "Nghệ thuật & Sưu tầm",
                "Túi xách & Thời trang",
                "Bất động sản",
                "Khác"
        );

        // Cập nhật số liệu ban đầu
        refreshStats();
    }

    // ===== XỬ LÝ SỰ KIỆN FORM =====

    /**
     * handlePostItem: Khi nhấn nút "ĐĂNG VẬT PHẨM ĐẤU GIÁ".
     * Kiểm tra dữ liệu nhập vào và thêm vật phẩm vào danh sách.
     */
    @FXML
    void handlePostItem(ActionEvent event) {

        // ─────────────────────────────
        // VALIDATE INPUT
        // ─────────────────────────────

        String name =
                txtItemName.getText().trim();

        String desc =
                txtDescription.getText().trim();

        String priceStr =
                txtStartPrice.getText()
                        .trim()
                        .replaceAll("[.,\\s]", "");

        String stepStr =
                txtMinStep.getText()
                        .trim()
                        .replaceAll("[.,\\s]", "");

        String durationStr =
                txtDurationHours.getText()
                        .trim();

        if (

                name.isEmpty()
                        || desc.isEmpty()
                        || priceStr.isEmpty()
                        || stepStr.isEmpty()
                        || durationStr.isEmpty()

        ) {

            showMessage(
                    "⚠ Vui lòng nhập đầy đủ thông tin!",
                    false
            );

            return;
        }

        // ─────────────────────────────
        // PARSE NUMBER
        // ─────────────────────────────

        double startPrice;
        double minStep;
        int durationMinutes;

        try {

            startPrice =
                    Double.parseDouble(priceStr);

            minStep =
                    Double.parseDouble(stepStr);

            durationMinutes =
                    Integer.parseInt(durationStr);

        } catch (NumberFormatException e) {

            showMessage(
                    "❌ Giá và thời gian phải là số!",
                    false
            );

            return;
        }

        // ─────────────────────────────
        // CONDITION
        // ─────────────────────────────

        String condition =
                "Đã qua sử dụng";

        if (rbNew.isSelected()) {

            condition = "Mới 100%";
        }

        if (rbLikeNew.isSelected()) {

            condition = "Như mới";
        }

        // ─────────────────────────────
        // CATEGORY
        // ─────────────────────────────

        String category =
                cmbCategory.getValue();

        if (category == null) {

            category = "Khác";
        }

        // ─────────────────────────────
        // CURRENT USER
        // ─────────────────────────────

        User user =
                RegisterController.currentUser;

        if (user == null) {

            showMessage(
                    "❌ Chưa đăng nhập!",
                    false
            );

            return;
        }

        int sellerId =
                Integer.parseInt(
                        user.getId()
                );

        // ─────────────────────────────
        // CREATE ITEM
        // ─────────────────────────────

        ItemDAO itemDAO =
                new ItemDAO();


        int itemId =

                itemDAO.addItem(

                        name,

                        desc,

                        startPrice,

                        category,

                        condition,

                        durationMinutes,

                        sellerId
                );

        if (itemId == -1) {

            showMessage(
                    "❌ Không thể tạo vật phẩm!",
                    false
            );

            return;
        }

        // ─────────────────────────────
        // CREATE AUCTION
        // ─────────────────────────────
        
        // Bỏ qua bước tạo auction riêng biệt vì items đã lưu thông tin auction (duration, status)

        // ─────────────────────────────
        // UPDATE UI
        // ─────────────────────────────

        String entry =

                String.format(

                        "🏷 %s | %s ₫ | %s | %d phút",

                        name,

                        currencyFormat.format(
                                (long) startPrice
                        ),

                        condition,

                        durationMinutes
                );

        listMyItems
                .getItems()
                .add(0, entry);

        totalItems++;

        activeCount++;

        refreshStats();

        // ─────────────────────────────
        // SUCCESS MESSAGE
        // ─────────────────────────────

        showMessage(

                "✅ Đăng vật phẩm thành công!",

                true
        );

        // ─────────────────────────────
        // CLEAR FORM
        // ─────────────────────────────

        clearFormFields();
    }
    @FXML
    void handleClearForm(ActionEvent event) {
        clearFormFields();
        lblMessage.setText("");
    }

    /**
     * handleFilter: Lọc danh sách vật phẩm theo từ khoá tìm kiếm.
     */
    @FXML
    void handleFilter(ActionEvent event) {
        String keyword = txtSearch.getText().trim().toLowerCase();
        // TODO: Kết nối database để lọc thật sự
        // Hiện tại chỉ hiển thị thông báo demo
        if (keyword.isEmpty()) {
            showMessage("💡 Nhập từ khoá để lọc danh sách.", false);
        } else {
            showMessage("🔍 Đang lọc theo: \"" + keyword + "\"", true);
        }
    }

    /**
     * handleViewItem: Xem chi tiết vật phẩm được chọn trong ListView.
     */
    @FXML
    void handleViewItem(ActionEvent event) {
        String selected = listMyItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠ Vui lòng chọn một vật phẩm để xem chi tiết.", false);
            return;
        }
        showMessage("👁 Đang xem: " + selected.substring(0, Math.min(60, selected.length())) + "...", true);
        // TODO: Mở cửa sổ chi tiết vật phẩm
    }

    /**
     * handleWithdrawItem: Rút vật phẩm khỏi sàn đấu giá.
     */
    @FXML
    void handleWithdrawItem(ActionEvent event) {
        int selectedIndex = listMyItems.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            showMessage("⚠ Vui lòng chọn vật phẩm muốn rút.", false);
            return;
        }

        // Xác nhận trước khi rút
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận rút vật phẩm");
        confirm.setHeaderText("Bạn có chắc muốn rút vật phẩm này khỏi sàn?");
        confirm.setContentText("Hành động này không thể hoàn tác nếu đã có người đặt giá.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                listMyItems.getItems().remove(selectedIndex);
                if (totalItems > 0) totalItems--;
                if (activeCount > 0) activeCount--;
                refreshStats();
                showMessage("✅ Đã rút vật phẩm khỏi sàn đấu giá.", true);
            }
        });
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

    private void clearFormFields() {
        txtItemName.clear();
        txtDescription.clear();
        txtStartPrice.clear();
        txtMinStep.clear();
        txtDurationHours.clear();
        cmbCategory.setValue(null);
        rbLikeNew.setSelected(true);
    }

    private void refreshStats() {
        lblItemCount.setText(totalItems + " vật phẩm");
        lblTotalItems.setText(String.valueOf(totalItems));
        lblActiveAuctions.setText(String.valueOf(activeCount));
        lblSoldItems.setText(String.valueOf(soldCount));
    }

    private void showMessage(String msg, boolean isSuccess) {
        lblMessage.setText(msg);
        lblMessage.setStyle(isSuccess
                ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
    }
}
