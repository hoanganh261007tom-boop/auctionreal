package org.example.auctionreal;

import database.UserDAO;
import user.Bidder;
import user.Seller;
import user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * RegisterController – Xử lý màn hình Đăng ký (register.fxml).
 * Luồng mới:
 *   1. Người dùng nhập username, password, chọn role (BIDDER/SELLER).
 *   2. Insert vào DB → lấy user_id thực (generated key).
 *   3. Tạo currentUser đúng type với ID thực.
 *   4. Redirect thẳng vào dashboard tương ứng (không qua role-selection).
 * => Không còn lỗi seller_id = 0 khi đăng sản phẩm.
 * => Role được lưu cố định trong DB, không thể thay đổi sau này.
 */
public class RegisterController {

    /** User hiện tại – dùng chung cho tất cả controller. */
    public static User currentUser;

    @FXML private TextField       txtUsername;
    @FXML private PasswordField   txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label           lblRoleDesc;
    @FXML private Label           lblMessage;

    @FXML
    public void initialize() {
        // Chỉ cho chọn BIDDER và SELLER — Admin chỉ được tạo bởi DBA trực tiếp
        cmbRole.getItems().addAll("NGƯỜI MUA (BIDDER)", "NGƯỜI BÁN (SELLER)");

        // Hiển thị mô tả khi chọn role
        cmbRole.setOnAction(e -> {
            String selected = cmbRole.getValue();
            if (selected == null) { lblRoleDesc.setText(""); return; }
            if (selected.contains("BIDDER")) {
                lblRoleDesc.setText("🔍 Bạn sẽ tham gia đấu giá, đặt giá cho các sản phẩm.");
                lblRoleDesc.setStyle("-fx-text-fill: #64b5f6; -fx-font-size: 11px;");
            } else {
                lblRoleDesc.setText("🏪 Bạn sẽ đăng sản phẩm lên sàn đấu giá.");
                lblRoleDesc.setStyle("-fx-text-fill: #ffb74d; -fx-font-size: 11px;");
            }
        });
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String roleDisplay = cmbRole.getValue();

        // ── Validate cơ bản ──
        if (username.isEmpty()) {
            showMessage("⚠ Vui lòng nhập tên đăng nhập!", false); return;
        }
        if (!username.matches("[a-zA-Z0-9_]{4,20}")) {
            showMessage("⚠ Tên đăng nhập 4–20 ký tự, chỉ dùng chữ/số/gạch dưới!", false); return;
        }
        if (password.length() < 4) {
            showMessage("⚠ Mật khẩu phải ít nhất 4 ký tự!", false); return;
        }
        if (password.contains(" ")) {
            showMessage("⚠ Mật khẩu không được chứa khoảng trắng!", false); return;
        }
        if (roleDisplay == null || roleDisplay.isEmpty()) {
            showMessage("⚠ Vui lòng chọn vai trò!", false); return;
        }

        // Chuyển đổi hiển thị → role DB
        String role = roleDisplay.contains("SELLER") ? "SELLER" : "BIDDER";

        UserDAO userDAO = new UserDAO();

        // Kiểm tra username đã tồn tại chưa (thử login không được — dùng hàm riêng)
        if (userDAO.isUsernameTaken(username)) {
            showMessage("❌ Tên đăng nhập đã tồn tại! Hãy chọn tên khác.", false);
            return;
        }

        // ── Đăng ký vào DB → lấy user_id thực ──
        int newUserId = userDAO.registerUser(username, password, role);
        if (newUserId <= 0) {
            showMessage("❌ Đăng ký thất bại! Tên đăng nhập có thể đã tồn tại.", false);
            return;
        }

        // ── Tạo currentUser với ID THỰC từ DB ──
        if ("SELLER".equals(role)) {
            currentUser = new Seller(newUserId, username, password);
            navigateTo(event, "seller-dashboard.fxml", "🏪 Seller Dashboard", 1200, 800);
        } else {
            currentUser = new Bidder(newUserId, username, password, 50_000_000.0);
            navigateTo(event, "bidder-dashboard.fxml", "🔍 Bidder Dashboard", 1200, 800);
        }
    }

    @FXML
    void handleGoToLogin(ActionEvent event) {
        navigateTo(event, "hello-view.fxml", "Đăng nhập", 500, 450);
    }

    // ── Tiện ích ──

    private void navigateTo(ActionEvent event, String fxmlFile, String title,
                             double width, double height) {
        try {
            URL location = getClass().getResource(fxmlFile);
            if (location == null) {
                // Thử đường dẫn tuyệt đối
                location = getClass().getResource("/org/example/auctionreal/" + fxmlFile);
            }
            if (location == null) {
                showMessage("❌ Không tìm thấy file giao diện: " + fxmlFile, false);
                return;
            }
            Parent root  = FXMLLoader.load(location);
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
            stage.setResizable(true);
            if (width >= 900) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            System.err.println("[RegisterController] Lỗi chuyển màn hình: " + e.getMessage());
            e.printStackTrace();
            showMessage("❌ Lỗi hệ thống khi chuyển màn hình!", false);
        }
    }

    private void showMessage(String msg, boolean ok) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle(ok
                    ? "-fx-text-fill: #4af0a0; -fx-font-size: 12px;"
                    : "-fx-text-fill: #ff7777; -fx-font-size: 12px;");
        }
    }
}