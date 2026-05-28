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
    // ĐĂNG SẢN PHẨM MỚI
    // =====================================================
    @FXML
    void handlePostItem(ActionEvent event) {
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
    // SỬA SẢN PHẨM
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

        // Điền thông tin vào form để sửa
        txtItemName.setText(selected.name);
        txtDescription.setText(selected.description != null ? selected.description : "");
        txtStartPrice.setText(String.valueOf((long) selected.startPrice));
        txtMinStep.setText(String.valueOf((long) selected.minStep));

        showMessage("✏ Đã tải thông tin sản phẩm. Sửa xong nhấn 'LƯU SỬA'.", true);

        // Đổi nút thành "Lưu sửa" tạm thời — dùng Alert để confirm
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sửa sản phẩm");
        confirm.setHeaderText("Sửa: " + selected.name);
        confirm.setContentText("Nhấn OK sau khi bạn đã chỉnh sửa thông tin trong form bên trái.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
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
                    boolean ok   = dao.updateItem(selected.itemId, newName, newDesc, price, step);
                    if (ok) {
                        showMessage("✅ Sửa sản phẩm thành công!", true);
                        clearFormFields();
                        loadMyItems();
                    } else {
                        showMessage("❌ Sửa thất bại!", false);
                    }
                } catch (NumberFormatException e) {
                    showMessage("❌ Giá không hợp lệ!", false);
                }
            }
        });
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
        clearFormFields();
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
