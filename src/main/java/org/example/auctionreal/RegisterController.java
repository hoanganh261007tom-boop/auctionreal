package org.example.auctionreal;
import user.Bidder;
import user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class RegisterController {

    public static User currentUser;

    @FXML private TextField txtId;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    void handleRegister(ActionEvent event) {
        String id = txtId.getText();
        String name = txtUsername.getText();
        String password = txtPassword.getText();

        if (id.isEmpty() || name.isEmpty() || password.length() < 4) {
            return;
        }

        currentUser = new Bidder(id, name, password, 0.0);

        try {
            switchToRoleSelection(event);
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void switchToRoleSelection(ActionEvent event) throws IOException {
        /**
         * SỬA LỖI: "Location is required"
         * Đảm bảo đường dẫn bắt đầu bằng dấu "/" và đi từ thư mục gốc của resources.
         */
        String fxmlPath = "/org/example/auctionreal/role-selection.fxml";
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
        }

        Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Lựa chọn vai trò người dùng");
        stage.show();
    }
}