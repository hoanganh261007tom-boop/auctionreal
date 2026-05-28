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
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class RegisterController {

    // Khai báo biến toàn cục
    public static User currentUser;

    @FXML private TextField txtId;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    void handleRegister(ActionEvent event) {
        String id = txtId.getText().trim();
        String name = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (id.isEmpty() || name.isEmpty() || password.length() < 4) {
            showAlert("Loi nhap lieu", "Vui long nhap day du thong tin (mat khau it nhat 4 ky tu).");
            return;
        }

        UserDAO userDAO = new UserDAO();
        String existingRole = userDAO.getRoleByLogin(name, password);

        if (existingRole != null) {
            System.out.println("Dang nhap thanh cong! Vai tro: " + existingRole);
            if (existingRole.equalsIgnoreCase("SELLER")) {
                currentUser = new Seller(id, name, password, existingRole);
            } else {
                currentUser = new Bidder(id, name, password, 50000000.0);
            }
            chuyenTrang(event);

        } else {
            // ĐÃ SỬA LỖI INT/BOOLEAN Ở ĐÂY: Ép kiểu để tương thích 100% với UserDAO của bạn
            int rowsAffected = userDAO.registerUser(name, password, "BIDDER");
            boolean isRegistered = (rowsAffected > 0);

            if (isRegistered) {
                System.out.println("Dang ky tai khoan moi thanh cong!");
                currentUser = new Bidder(id, name, password, 50000000.0);
                chuyenTrang(event);
            } else {
                showAlert("Loi he thong", "Tai khoan da ton tai hoac mat khau sai!");
            }
        }
    }

    private void chuyenTrang(ActionEvent event) {
        try {
            switchToRoleSelection(event);
        } catch (IOException e) {
            System.err.println("Loi chuyen man hinh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void switchToRoleSelection(ActionEvent event) throws IOException {
        String fxmlPath = "/org/example/auctionreal/role-selection.fxml";
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            throw new IOException("Khong tim thay file FXML: " + fxmlPath);
        }

        Parent root = FXMLLoader.load(location);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Lua chon vai tro nguoi dung");
        stage.show();
    }

    // ĐÂY LÀ HÀM MỚI ĐƯỢC THÊM VÀO TRƯỚC DẤU NGOẶC KẾT THÚC
    @FXML
    void handleGoToLogin(ActionEvent event) {
        try {
            String fxmlPath = "/org/example/auctionreal/hello-view.fxml";
            URL location = getClass().getResource(fxmlPath);

            if (location == null) {
                System.err.println("Khong tim thay file FXML: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(location);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dang nhap");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}