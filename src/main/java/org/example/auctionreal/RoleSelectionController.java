package org.example.auctionreal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import user.Admin;
import user.Bidder;
import user.Seller;
import user.User;
import javafx.scene.control.Alert;

import java.io.IOException;

public class RoleSelectionController {

    private static final double INITIAL_BALANCE = 50_000_000.0;

    @FXML private Label lblWelcome;

    @FXML
    public void initialize() {
        User user = RegisterController.currentUser;
        if (user != null) {
            lblWelcome.setText("Chào " + user.getUsername() + "! Hãy chọn vai trò của bạn:");

            // Nếu là ADMIN → tự động chuyển vào Admin Dashboard
            if ("ADMIN".equals(user.getRole())) {
                lblWelcome.setText("Chào Admin " + user.getUsername() + "! Đang vào Admin Panel...");
            }
        }
    }

    @FXML
    void handleSelectSeller(ActionEvent event) {
        User temp = RegisterController.currentUser;
        RegisterController.currentUser = new Seller(
                String.valueOf(temp.getId()), temp.getUsername(), "n/a", "SELLER");
        try {
            switchToScreen(event, "seller-dashboard.fxml", "🏪 Seller Dashboard", 1200, 800);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSelectBidder(ActionEvent event) {
        User temp = RegisterController.currentUser;
        RegisterController.currentUser = new Bidder(
                temp.getId(), temp.getUsername(), "n/a", INITIAL_BALANCE);
        try {
            switchToScreen(event, "bidder-dashboard.fxml", "🔍 Bidder Dashboard", 1200, 800);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Nút Admin – chỉ hiển thị khi role là ADMIN
     */
    @FXML
    void handleSelectAdmin(ActionEvent event) {
        User temp = RegisterController.currentUser;

        // Kiểm tra role từ DB
        if (!"ADMIN".equals(temp.getRole())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Từ chối truy cập");
            alert.setHeaderText("❌ Bạn không có quyền Admin!");
            alert.setContentText("Chỉ tài khoản có vai trò ADMIN mới vào được.");
            alert.showAndWait();
            return;
        }

        RegisterController.currentUser = new Admin(
                String.valueOf(temp.getId()), temp.getUsername(), "n/a", "ADMIN");
        try {
            switchToScreen(event, "admin-dashboard.fxml", "🛡 Admin Panel", 1100, 700);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchToScreen(ActionEvent event, String fxmlFile, String title,
                                double width, double height) throws IOException {
        java.net.URL url = getClass().getResource(fxmlFile);
        if (url == null) {
            System.err.println("Không tìm thấy: " + fxmlFile);
            return;
        }
        Parent root  = FXMLLoader.load(url);
        Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, width, height));
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        if (width >= 900) stage.setMaximized(true);
        stage.show();
    }

    @FXML
    void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("/org/example/auctionreal/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng ký tài khoản");
        stage.show();
    }
}
