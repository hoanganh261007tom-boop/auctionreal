package org.example.auctionreal;
import database.UserDAO;
import user.Bidder;
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

    @FXML private TextField txtId;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    public static User currentUser;

    @FXML
    void handleRegister(ActionEvent event) {
        String id       = txtId.getText().trim();
        String name     = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // --- Validate cơ bản ---
        if (id.isEmpty() || name.isEmpty() || password.length() < 4) {
            showAlert(Alert.AlertType.WARNING,
                    "Thông tin không hợp lệ",
                    "Vui lòng kiểm tra lại!",
                    "• ID và Tên không được để trống.\n• Mật khẩu phải có ít nhất 4 ký tự.");
            return;
        }

        // --- Lưu vào Database ---
        UserDAO dao  = new UserDAO();
        int generatedId = dao.registerUser(name, password, "BIDDER"); // role mặc định BIDDER

        if (generatedId == -1) {
            showAlert(Alert.AlertType.ERROR,
                    "Đăng ký thất bại",
                    "Không thể lưu tài khoản!",
                    "Tên tài khoản đã tồn tại hoặc không kết nối được Database.\nKiểm tra lại MySQL và thông tin trong DatabaseConnection.java.");
            return;
        }

        // --- Lưu vào bộ nhớ để dùng ở các màn hình sau ---
        currentUser = new Bidder(generatedId, name, password, 0.0);
        System.out.println("[Register] Đăng ký thành công: " + currentUser);

        showAlert(Alert.AlertType.INFORMATION,
                "Đăng ký thành công",
                "Chào mừng " + name + "!",
                "Tài khoản của bạn đã được tạo thành công.\nBây giờ hãy chọn vai trò của bạn.");

        try {
            switchToRoleSelection(event);
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Hiện hộp thoại thông báo tiện ích. */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
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
