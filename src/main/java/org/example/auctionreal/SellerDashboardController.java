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
        // --- Validate ---
        String name = txtItemName.getText().trim();
        String desc = txtDescription.getText().trim();
        String priceStr = txtStartPrice.getText().trim().replaceAll("[.,\\s]", "");
        String stepStr  = txtMinStep.getText().trim().replaceAll("[.,\\s]", "");
        String hoursStr = txtDurationHours.getText().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty()
                || stepStr.isEmpty() || hoursStr.isEmpty()) {
            showMessage("⚠ Vui lòng điền đầy đủ các trường bắt buộc (*)", false);
            return;
        }

        double startPrice, minStep;
        int durationMins;
        try {
            startPrice    = Double.parseDouble(priceStr);
            minStep       = Double.parseDouble(stepStr);
            durationMins  = Integer.parseInt(hoursStr);
        } catch (NumberFormatException e) {
            showMessage("❌ Giá khởi điểm, bước giá và thời gian phải là số!", false);
            return;
        }

        if (startPrice <= 0 || minStep <= 0 || durationMins <= 0) {
            showMessage("❌ Giá và thời gian phải lớn hơn 0!", false);
            return;
        }

        // --- Lấy trạng thái vật phẩm ---
        String condition = "Đã qua sử dụng";
        if (rbNew.isSelected())     condition = "Mới 100%";
        if (rbLikeNew.isSelected()) condition = "Như mới";

        // --- Lấy danh mục ---
        String category = cmbCategory.getValue() != null ? cmbCategory.getValue() : "Khác";

        // --- Tạo dòng hiển thị trong ListView ---
        String seller = (RegisterController.currentUser != null)
                ? RegisterController.currentUser.getUsername()
                : "Ẩn danh";

        String entry = String.format(
                "🏷 %s  |  Giá KĐ: %s ₫  |  %s  |  %d phút  [%s]",
                name,
                currencyFormat.format((long) startPrice),
                condition,
                durationMins,
                category
        );
        listMyItems.getItems().add(0, entry);

        // --- Cập nhật thống kê ---
        totalItems++;
        activeCount++;
        refreshStats();

        showMessage("✅ Đã đăng vật phẩm \"" + name + "\" lên sàn đấu giá thành công!", true);

        // --- Gán thời gian cho AuctionController (phút) ---
        AuctionController.selectedDuration = durationMins;

        // --- Xoá form sau khi đăng thành công ---
        clearFormFields();

        System.out.println("Seller [" + seller + "] đã đăng vật phẩm: " + name
                + " | Giá KĐ: " + startPrice
                + " | Bước: " + minStep
                + " | Thời gian: " + durationMins + " phút"
                + " | Tình trạng: " + condition
                + " | Danh mục: " + category);
    }

    /**
     * handleClearForm: Xoá trắng toàn bộ form nhập liệu.
     */
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
