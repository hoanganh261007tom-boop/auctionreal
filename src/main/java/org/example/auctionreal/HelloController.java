package org.example.auctionreal;

import database.UserDAO;
import user.Admin;
import user.Bidder;
import user.Seller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * HelloController – Xử lý màn hình Đăng nhập (hello-view.fxml).
 * Luồng đăng nhập:
 *   ADMIN  → admin-dashboard.fxml
 *   SELLER → seller-dashboard.fxml
 *   BIDDER → bidder-dashboard.fxml
 */
public class HelloController {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblMessage;

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("⚠ Vui lòng nhập tên đăng nhập và mật khẩu!", false);
            return;
        }

        UserDAO dao = new UserDAO();
        UserDAO.LoginResult result = dao.getUserByLogin(username, password);

        if (result == null) {
            showMessage("❌ Sai tài khoản hoặc mật khẩu. Vui lòng thử lại.", false);
            return;
        }

        showMessage("✅ Đăng nhập thành công! Đang chuyển hướng...", true);

        // ── Phân luồng theo role từ DB ──
        String role = result.role != null ? result.role.toUpperCase() : "BIDDER";

        switch (role) {
            case "ADMIN":
                RegisterController.currentUser = new Admin(result.id, username, password);
                navigateTo(event, "admin-dashboard.fxml", "🛡 Admin Panel", 1100, 700);
                break;

            case "SELLER":
                RegisterController.currentUser = new Seller(result.id, username, password);
                navigateTo(event, "seller-dashboard.fxml", "🏪 Seller Dashboard", 1200, 800);
                break;

            default: // BIDDER hoặc bất kỳ role nào khác
                double balance = result.balance > 0 ? result.balance : 50_000_000.0;
                RegisterController.currentUser = new Bidder(result.id, username, password, balance);
                navigateTo(event, "bidder-dashboard.fxml", "🔍 Bidder Dashboard", 1200, 800);
                break;
        }
    }

    @FXML
    protected void onGoToRegister(ActionEvent event) {
        navigateTo(event, "register.fxml", "Đăng ký tài khoản", 500, 680);
    }

    // ── Tiện ích ──

    private void navigateTo(ActionEvent event, String fxmlFile, String title,
                             double width, double height) {
        try {
            java.net.URL fxmlUrl = getClass().getResource(fxmlFile);
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng",
                        "Không tìm thấy file: " + fxmlFile,
                        "Kiểm tra lại tên file trong thư mục resources.");
                return;
            }
            Parent root  = FXMLLoader.load(fxmlUrl);
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
            stage.setResizable(true);
            if (width >= 900) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, boolean isSuccess) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle(isSuccess
                    ? "-fx-text-fill: #4af0a0; -fx-font-size: 13px;"
                    : "-fx-text-fill: #ff7777; -fx-font-size: 13px;");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
