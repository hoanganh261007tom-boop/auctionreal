package org.example.auctionreal;

import database.dao.AuctionDAO;
import database.dao.ItemDAO;
import database.dao.ItemDAO.AuctionItemInfo;
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
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SellerDashboardController {

    // ── Form đăng sản phẩm ──
    @FXML private Label    lblUserInfo;
    @FXML private Label    lblFormTitle;    // tiêu đề form (thay đổi khi edit)
    @FXML private Button   btnSubmitForm;  // nút đăng/lưu (thay đổi khi edit)
    @FXML private TextField txtItemName;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtMinStep;
    @FXML private TextField txtDurationHours;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private RadioButton rbNew, rbLikeNew, rbUsed;
    @FXML private Label lblMessage;

    // ── Danh sách sản phẩm ──
    @FXML private Label lblItemCount;
    @FXML private Label lblTotalItems;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblSoldItems;
    @FXML private TextField txtSearch;
    @FXML private ListView<String> listMyItems;

    private final ToggleGroup conditionGroup = new ToggleGroup();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /** Danh sách item từ DB của seller này */
    private List<AuctionItemInfo> myItems;

    /** ID của item đang được chỉnh sửa; -1 nếu ở chế độ đăng mới */
    private int editingItemId = -1;

    // =====================================================
    // INITIALIZE
    // =====================================================
    @FXML
    public void initialize() {
        User user = RegisterController.currentUser;
        lblUserInfo.setText(user != null
                ? "🏪 " + user.getUsername() + "  |  SELLER"
                : "🏪 Người bán");

        rbNew.setToggleGroup(conditionGroup);
        rbLikeNew.setToggleGroup(conditionGroup);
        rbUsed.setToggleGroup(conditionGroup);
        rbLikeNew.setSelected(true);

        cmbCategory.getItems().addAll(
                "Đồng hồ cao cấp", "Điện tử & Công nghệ",
                "Trang sức & Đá quý", "Xe cộ",
                "Nghệ thuật & Sưu tầm", "Túi xách & Thời trang",
                "Bất động sản", "Khác"
        );

        loadMyItems();
    }

    // =====================================================
    // LOAD ITEMS TỪ DB
    // =====================================================
    private void loadMyItems() {
        User user = RegisterController.currentUser;
        if (user == null) return;

        ItemDAO dao = new ItemDAO();
        myItems = dao.getItemsBySeller(user.getId());

        listMyItems.getItems().clear();
        for (AuctionItemInfo item : myItems) {
            listMyItems.getItems().add(formatItem(item));
        }
        refreshStats();
    }

    private String formatItem(AuctionItemInfo item) {
        return String.format("🏷 %s  |  %s ₫  |  Bước: %s ₫  |  %s",
                item.name,
                fmt.format((long) item.currentPrice),
                fmt.format((long) item.minStep),
                item.status);
    }

    // =====================================================
    // ĐĂNG SẢN PHẨM MỚI  hoặc  LƯU SỬA (tuỳ editingItemId)
    // =====================================================
    @FXML
    void handlePostItem(ActionEvent event) {
        // ── Nếu đang ở chế độ chỉnh sửa, chuyển sang lưu sửa ──
        if (editingItemId != -1) {
            handleSaveEdit();
            return;
        }

        // ── Chế độ đăng mới ──
        String name       = txtItemName.getText().trim();
        String desc       = txtDescription.getText().trim();
        String priceStr   = txtStartPrice.getText().trim().replaceAll("[.,\\s]", "");
        String stepStr    = txtMinStep.getText().trim().replaceAll("[.,\\s]", "");
        String durStr     = txtDurationHours.getText().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty()
                || stepStr.isEmpty() || durStr.isEmpty()) {
            showMessage("⚠ Vui lòng nhập đầy đủ thông tin!", false);
            return;
        }

        double startPrice, minStep;
        int durationMinutes;
        try {
            startPrice      = Double.parseDouble(priceStr);
            minStep         = Double.parseDouble(stepStr);
            durationMinutes = Integer.parseInt(durStr);
        } catch (NumberFormatException e) {
            showMessage("❌ Giá và thời gian phải là số!", false);
            return;
        }

        String condition = rbNew.isSelected() ? "Mới 100%"
                : rbLikeNew.isSelected() ? "Như mới" : "Đã qua sử dụng";
        String category  = cmbCategory.getValue() != null ? cmbCategory.getValue() : "Khác";

        User user = RegisterController.currentUser;
        if (user == null) { showMessage("❌ Chưa đăng nhập!", false); return; }

        int sellerId = user.getId();

        ItemDAO itemDAO = new ItemDAO();
        int itemId = itemDAO.addItem(name, desc, startPrice, category,
                condition, durationMinutes, sellerId, minStep);
        if (itemId == -1) { showMessage("❌ Không thể tạo vật phẩm!", false); return; }

        AuctionDAO auctionDAO = new AuctionDAO();
        Timestamp now     = new Timestamp(System.currentTimeMillis());
        Timestamp endTime = new Timestamp(System.currentTimeMillis() + (long) durationMinutes * 60 * 1000);
        auctionDAO.createAuction(itemId, now, endTime, startPrice);

        showMessage("✅ Đăng vật phẩm thành công!", true);
        clearFormFields();
        loadMyItems(); // Reload danh sách từ DB
    }

    // =====================================================
    // SỬA SẢN PHẨM – Tải thông tin lên form và bật chế độ edit
    // =====================================================
    @FXML
    void handleEditItem(ActionEvent event) {
        int idx = listMyItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= myItems.size()) {
            showMessage("⚠ Vui lòng chọn sản phẩm muốn sửa!", false);
            return;
        }

        AuctionItemInfo selected = myItems.get(idx);

        // Kiểm tra đã có người đặt giá chưa
        if (selected.currentPrice > selected.startPrice) {
            showMessage("❌ Không thể sửa sản phẩm đã có người đặt giá!", false);
            return;
        }

        // Kiểm tra trạng thái: chỉ cho phép sửa OPEN
        if (!"OPEN".equals(selected.status)) {
            showMessage("❌ Chỉ có thể sửa sản phẩm đang mở đấu giá!", false);
            return;
        }

        // Lưu ID đang sửa và điền dữ liệu vào form
        editingItemId = selected.itemId;
        txtItemName.setText(selected.name);
        txtDescription.setText(selected.description != null ? selected.description : "");
        txtStartPrice.setText(String.valueOf((long) selected.startPrice));
        txtMinStep.setText(String.valueOf((long) selected.minStep));
        // txtDurationHours không thể sửa (phiên đấu giá đã bắt đầu)
        txtDurationHours.setDisable(true);
        txtDurationHours.setText("(không đổi)");

        // Chuyển form sang chế độ chỉnh sửa
        enterEditMode(selected.name);
    }

    /** Lưu thay đổi khi đang ở chế độ edit */
    private void handleSaveEdit() {
        String newName  = txtItemName.getText().trim();
        String newDesc  = txtDescription.getText().trim();
        String newPrice = txtStartPrice.getText().trim().replaceAll("[.,\\s]", "");
        String newStep  = txtMinStep.getText().trim().replaceAll("[.,\\s]", "");

        if (newName.isEmpty() || newDesc.isEmpty()
                || newPrice.isEmpty() || newStep.isEmpty()) {
            showMessage("⚠ Vui lòng điền đầy đủ thông tin!", false);
            return;
        }

        try {
            double price = Double.parseDouble(newPrice);
            double step  = Double.parseDouble(newStep);
            ItemDAO dao  = new ItemDAO();
            boolean ok   = dao.updateItem(editingItemId, newName, newDesc, price, step);
            if (ok) {
                showMessage("✅ Sửa sản phẩm thành công!", true);
                exitEditMode();
                loadMyItems();
            } else {
                showMessage("❌ Sửa thất bại! Vui lòng thử lại.", false);
            }
        } catch (NumberFormatException e) {
            showMessage("❌ Giá không hợp lệ!", false);
        }
    }

    /** Bật chế độ chỉnh sửa: đổi tiêu đề + nút */
    private void enterEditMode(String itemName) {
        lblFormTitle.setText("CHỈNH SỬA VẬT PHẨM");
        lblFormTitle.setStyle("-fx-text-fill: #4af0a0; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnSubmitForm.setText("💾  LƯU THAY ĐỔI");
        btnSubmitForm.setStyle("-fx-background-color: #4af0a0; -fx-text-fill: #0f0f1a; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-cursor: hand;");
        showMessage("✏ Đang sửa: " + itemName + "  |  Chỉnh sửa thông tin rồi nhấn 'LƯU THAY ĐỔI'.", true);
    }

    /** Thoát chế độ chỉnh sửa: reset tiêu đề + nút + form */
    private void exitEditMode() {
        editingItemId = -1;
        txtDurationHours.setDisable(false);
        txtDurationHours.clear();
        lblFormTitle.setText("ĐĂNG VẬT PHẨM ĐẤU GIÁ");
        lblFormTitle.setStyle("-fx-text-fill: #f0c040; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnSubmitForm.setText("🚀  ĐĂNG VẬT PHẨM LÊN SÀN ĐẤU GIÁ");
        btnSubmitForm.setStyle("-fx-background-color: #f0c040; -fx-text-fill: #0f0f1a; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-cursor: hand;");
        clearFormFields();
    }

    // =====================================================
    // XÓA SẢN PHẨM
    // =====================================================
    @FXML
    void handleWithdrawItem(ActionEvent event) {
        int idx = listMyItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= myItems.size()) {
            showMessage("⚠ Vui lòng chọn sản phẩm muốn xóa!", false);
            return;
        }

        AuctionItemInfo selected = myItems.get(idx);

        // Cảnh báo nếu đã có người đặt giá
        String warning = selected.currentPrice > selected.startPrice
                ? "\n⚠ Cảnh báo: Đã có người đặt giá! Xóa sẽ hủy toàn bộ lịch sử đặt giá."
                : "";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + selected.name + "?");
        confirm.setContentText("Hành động này không thể hoàn tác." + warning);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ItemDAO dao = new ItemDAO();
                boolean ok  = dao.deleteItem(selected.itemId);
                if (ok) {
                    showMessage("✅ Đã xóa sản phẩm: " + selected.name, true);
                    loadMyItems();
                } else {
                    showMessage("❌ Xóa thất bại!", false);
                }
            }
        });
    }

    // =====================================================
    // TÌM KIẾM
    // =====================================================
    @FXML
    void handleFilter(ActionEvent event) {
        String keyword = txtSearch.getText().trim().toLowerCase();
        listMyItems.getItems().clear();
        for (AuctionItemInfo item : myItems) {
            if (keyword.isEmpty() || item.name.toLowerCase().contains(keyword)) {
                listMyItems.getItems().add(formatItem(item));
            }
        }
    }

    @FXML
    void handleViewItem(ActionEvent event) {
        int idx = listMyItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= myItems.size()) {
            showMessage("⚠ Chọn sản phẩm để xem chi tiết!", false);
            return;
        }
        AuctionItemInfo item = myItems.get(idx);
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Chi tiết sản phẩm");
        info.setHeaderText(item.name);
        info.setContentText(
                "Mô tả: " + item.description + "\n" +
                        "Giá khởi điểm: " + fmt.format((long) item.startPrice) + " ₫\n" +
                        "Giá hiện tại: "  + fmt.format((long) item.currentPrice) + " ₫\n" +
                        "Bước giá: "      + fmt.format((long) item.minStep) + " ₫\n" +
                        "Trạng thái: "    + item.status
        );
        info.showAndWait();
    }

    @FXML
    void handleClearForm(ActionEvent event) {
        if (editingItemId != -1) {
            // Hủy chỉnh sửa, về chế độ đăng mới
            exitEditMode();
        } else {
            clearFormFields();
        }
        lblMessage.setText("");
    }

    @FXML
    void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(false);
        stage.setScene(new Scene(root, 600, 400));
        stage.setMinWidth(600); stage.setMinHeight(400);
        stage.setTitle("Lựa chọn vai trò");
        stage.show();
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private void refreshStats() {
        int total  = myItems != null ? myItems.size() : 0;
        long active = myItems != null
                ? myItems.stream().filter(i -> "OPEN".equals(i.status)).count() : 0;
        long sold  = myItems != null
                ? myItems.stream().filter(i -> "CLOSED".equals(i.status)).count() : 0;

        lblItemCount.setText(total + " vật phẩm");
        lblTotalItems.setText(String.valueOf(total));
        lblActiveAuctions.setText(String.valueOf(active));
        lblSoldItems.setText(String.valueOf(sold));
    }

    private void clearFormFields() {
        txtItemName.clear();
        txtDescription.clear();
        txtStartPrice.clear();
        txtMinStep.clear();
        txtDurationHours.clear();
        cmbCategory.setValue(null);
        rbLikeNew.setSelected(true);
    }

    private void showMessage(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle(ok
                ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}
