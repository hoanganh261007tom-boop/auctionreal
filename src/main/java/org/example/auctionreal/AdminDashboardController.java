package org.example.auctionreal;

import database.DatabaseConnection;
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
import java.sql.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * AdminDashboardController – Màn hình quản trị hệ thống.
 * Chức năng: Xem danh sách user, xóa user, xem/xóa sản phẩm, thống kê.
 */
public class AdminDashboardController {

    // ── Thống kê ──
    @FXML private Label lblAdminInfo;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblTotalBids;

    // ── Quản lý Users ──
    @FXML private ListView<String> listUsers;
    @FXML private TextField txtSearchUser;
    @FXML private Label lblUserDetail;

    // ── Quản lý Items ──
    @FXML private ListView<String> listItems;
    @FXML private TextField txtSearchItem;
    @FXML private Label lblItemDetail;

    // ── Message ──
    @FXML private Label lblMessage;

    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // Lưu danh sách để dùng khi xóa
    private java.util.List<int[]> userIds   = new java.util.ArrayList<>(); // [user_id]
    private List<AuctionItemInfo> allItems  = new java.util.ArrayList<>();

    // =====================================================
    // INITIALIZE
    // =====================================================
    @FXML
    public void initialize() {
        User user = RegisterController.currentUser;
        lblAdminInfo.setText(user != null
                ? "🛡 " + user.getUsername() + "  |  ADMIN"
                : "🛡 Admin");

        loadStats();
        loadUsers();
        loadItems();
    }

    // =====================================================
    // THỐNG KÊ
    // =====================================================
    private void loadStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Tổng users
            ResultSet r1 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users");
            if (r1.next()) lblTotalUsers.setText(r1.getInt(1) + " người dùng");

            // Tổng items
            ResultSet r2 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM items");
            if (r2.next()) lblTotalItems.setText(r2.getInt(1) + " sản phẩm");

            // Tổng auctions
            ResultSet r3 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM auctions");
            if (r3.next()) lblTotalAuctions.setText(r3.getInt(1) + " phiên đấu giá");

            // Tổng bids
            ResultSet r4 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM bids");
            if (r4.next()) lblTotalBids.setText(r4.getInt(1) + " lượt đặt giá");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // QUẢN LÝ USERS
    // =====================================================
    private void loadUsers() {
        listUsers.getItems().clear();
        userIds.clear();

        String sql = "SELECT user_id, username, role FROM users ORDER BY user_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int    uid  = rs.getInt("user_id");
                String name = rs.getString("username");
                String role = rs.getString("role");
                userIds.add(new int[]{uid});
                listUsers.getItems().add(
                        String.format("#%d  |  %s  |  %s", uid, name, role)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSearchUser(ActionEvent event) {
        String keyword = txtSearchUser.getText().trim().toLowerCase();
        listUsers.getItems().clear();
        userIds.clear();

        String sql = "SELECT user_id, username, role FROM users " +
                "WHERE LOWER(username) LIKE ? ORDER BY user_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int    uid  = rs.getInt("user_id");
                String name = rs.getString("username");
                String role = rs.getString("role");
                userIds.add(new int[]{uid});
                listUsers.getItems().add(
                        String.format("#%d  |  %s  |  %s", uid, name, role)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleDeleteUser(ActionEvent event) {
        int idx = listUsers.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= userIds.size()) {
            showMessage("⚠ Chọn người dùng muốn xóa!", false);
            return;
        }

        int userId = userIds.get(idx)[0];

        // Không cho xóa chính mình
        User current = RegisterController.currentUser;
        if (current != null && current.getId() == userId) {
            showMessage("❌ Không thể xóa tài khoản của chính mình!", false);
            return;
        }

        String selectedText = listUsers.getItems().get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa người dùng: " + selectedText + "?");
        confirm.setContentText("Toàn bộ sản phẩm và lịch sử đấu giá của người này cũng sẽ bị xóa.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    // Xóa theo thứ tự tránh lỗi foreign key
                    conn.createStatement().executeUpdate(
                            "DELETE FROM bids WHERE bidder_id = " + userId);
                    conn.createStatement().executeUpdate(
                            "DELETE FROM watchlist WHERE user_id = " + userId);
                    conn.createStatement().executeUpdate(
                            "DELETE FROM bids WHERE auction_id IN " +
                                    "(SELECT auction_id FROM auctions WHERE item_id IN " +
                                    "(SELECT item_id FROM items WHERE seller_id = " + userId + "))");
                    conn.createStatement().executeUpdate(
                            "DELETE FROM auctions WHERE item_id IN " +
                                    "(SELECT item_id FROM items WHERE seller_id = " + userId + ")");
                    conn.createStatement().executeUpdate(
                            "DELETE FROM items WHERE seller_id = " + userId);
                    conn.createStatement().executeUpdate(
                            "DELETE FROM users WHERE user_id = " + userId);
                    conn.commit();

                    showMessage("✅ Đã xóa người dùng thành công!", true);
                    loadUsers();
                    loadStats();
                } catch (SQLException e) {
                    showMessage("❌ Xóa thất bại: " + e.getMessage(), false);
                }
            }
        });
    }

    // =====================================================
    // QUẢN LÝ ITEMS
    // =====================================================
    private void loadItems() {
        listItems.getItems().clear();
        ItemDAO dao = new ItemDAO();
        allItems = dao.getAllAuctionItems();
        for (AuctionItemInfo item : allItems) {
            listItems.getItems().add(String.format(
                    "#%d  |  %s  |  %s ₫  |  %s  |  👤 %s",
                    item.itemId, item.name,
                    fmt.format((long) item.currentPrice),
                    item.status, item.sellerName
            ));
        }
    }

    @FXML
    void handleSearchItem(ActionEvent event) {
        String keyword = txtSearchItem.getText().trim().toLowerCase();
        listItems.getItems().clear();
        for (AuctionItemInfo item : allItems) {
            if (keyword.isEmpty() || item.name.toLowerCase().contains(keyword)) {
                listItems.getItems().add(String.format(
                        "#%d  |  %s  |  %s ₫  |  %s  |  👤 %s",
                        item.itemId, item.name,
                        fmt.format((long) item.currentPrice),
                        item.status, item.sellerName
                ));
            }
        }
    }

    @FXML
    void handleViewItemDetail(ActionEvent event) {
        int idx = listItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allItems.size()) {
            showMessage("⚠ Chọn sản phẩm để xem chi tiết!", false);
            return;
        }
        AuctionItemInfo item = allItems.get(idx);
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Chi tiết sản phẩm");
        info.setHeaderText(item.name);
        info.setContentText(
                "ID: #" + item.itemId + "\n" +
                        "Mô tả: " + (item.description != null ? item.description : "Không có") + "\n" +
                        "Giá khởi điểm: " + fmt.format((long) item.startPrice) + " ₫\n" +
                        "Giá hiện tại: "  + fmt.format((long) item.currentPrice) + " ₫\n" +
                        "Bước giá: "      + fmt.format((long) item.minStep) + " ₫\n" +
                        "Trạng thái: "    + item.status + "\n" +
                        "Người bán: "     + item.sellerName
        );
        info.showAndWait();
    }

    @FXML
    void handleDeleteItem(ActionEvent event) {
        int idx = listItems.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allItems.size()) {
            showMessage("⚠ Chọn sản phẩm muốn xóa!", false);
            return;
        }
        AuctionItemInfo item = allItems.get(idx);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + item.name + "?");
        confirm.setContentText("Toàn bộ phiên đấu giá và lịch sử đặt giá sẽ bị xóa.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ItemDAO dao = new ItemDAO();
                boolean ok  = dao.deleteItem(item.itemId);
                if (ok) {
                    showMessage("✅ Đã xóa sản phẩm: " + item.name, true);
                    loadItems();
                    loadStats();
                } else {
                    showMessage("❌ Xóa thất bại!", false);
                }
            }
        });
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadStats();
        loadUsers();
        loadItems();
        showMessage("🔄 Đã làm mới dữ liệu!", true);
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

    private void showMessage(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle(ok
                ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff7777; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}
