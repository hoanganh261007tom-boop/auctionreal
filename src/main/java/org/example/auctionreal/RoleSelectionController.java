package org.example.auctionreal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import user.Bidder;
import user.Seller;
import user.User;

import java.io.IOException;

/**
 * RoleSelectionController: Xử lý logic khi người dùng chọn làm Người bán hoặc Người mua.
 */
public class RoleSelectionController {

    @FXML
    private Label lblWelcome;

    /**
     * initialize(): Chạy tự động khi giao diện được nạp.
     */
    @FXML
    public void initialize() {
        // Lấy thông tin người dùng từ màn hình đăng ký
        if (RegisterController.currentUser != null) {
            lblWelcome.setText("Chào " + RegisterController.currentUser.getUsername() + "! Hãy chọn vai trò của bạn:");
        }
    }

    @FXML
    void handleSelectSeller(ActionEvent event) {
        User temp = RegisterController.currentUser;
        // Chuyển đổi sang đối tượng Seller (giữ ID, Name từ Register)
        RegisterController.currentUser = new Seller(temp.getId(), temp.getUsername(), "n/a", "Seller");
        System.out.println("Hệ thống: Bạn đã chọn vai trò SELLER");
        // Chuyển sang Seller Dashboard
        try {
            switchToScreen(event, "seller-dashboard.fxml", "🏪 Seller Dashboard", 1000, 700);
        } catch (IOException e) {
            System.err.println("Lỗi không chuyển được trang: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleSelectBidder(ActionEvent event) {
        User temp = RegisterController.currentUser;
        // Chuyển đổi sang đối tượng Bidder với số dư mặc định 50.000.000 ₫
        RegisterController.currentUser = new Bidder(temp.getId(), temp.getUsername(), "n/a", 50_000_000.0);
        System.out.println("Hệ thống: Bạn đã chọn vai trò BIDDER");
        // Chuyển sang Bidder Dashboard
        try {
            switchToScreen(event, "bidder-dashboard.fxml", "🔍 Bidder Dashboard", 1000, 700);
        } catch (IOException e) {
            System.err.println("Lỗi không chuyển được trang: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * switchToScreen: Hàm tiện ích chuyển sang bất kỳ màn hình FXML nào.
     */
    private void switchToScreen(ActionEvent event, String fxmlFile, String title,
                                double width, double height) throws IOException {
        java.net.URL fxmlUrl = getClass().getResource(fxmlFile);
        if (fxmlUrl == null) {
            System.err.println("Lỗi: Không tìm thấy " + fxmlFile + "!");
            return;
        }
        Parent root = FXMLLoader.load(fxmlUrl);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, width, height));
        stage.setTitle(title);
        stage.show();
    }

    @FXML
    void handleBack(ActionEvent event) throws IOException {
        // Quay lại trang đăng ký
        Parent root = FXMLLoader.load(getClass().getResource("/org/example/auctionreal/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng ký tài khoản");
        stage.show();
    }
}
