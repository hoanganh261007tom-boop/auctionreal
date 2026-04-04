package org.example.auctionreal;
import javafx.scene.control.PasswordField;
import user.Bidder;
//Cho phép bạn sử dụng lớp Bidder để tạo mới người dùng khi họ nhấn đăng ký.
import user.User;
//Cho phép bạn sử dụng lớp cha User để làm biến lưu trữ chung
import javafx.event.ActionEvent;
//Chứa thông tin về sự kiện "nhấn nút". Khi bạn bấm nút, Java gửi một đối tượng này vào hàm để bạn biết ai vừa bấm và bấm lúc nào.
import javafx.fxml.FXML;
//Đây là nhãn dán quan trọng nhất. Nó dùng để đánh dấu các biến (txtId, txtUsername) hoặc hàm (handleRegister) để file FXML có thể "nhìn thấy" và kết nối vào code Java.
import javafx.fxml.FXMLLoader;
//Đây là cái "máy nạp". Nó có nhiệm vụ đọc file .fxml từ ổ cứng và biến nó thành các đối tượng giao diện trên màn hình.
import javafx.scene.Node;
//Là thành phần cơ bản nhất (Nút bấm, Ô nhập đều là Node). Trong code của bạn, chúng ta dùng nó để tìm ra cái Stage đang chứa cái nút vừa được bấm.
import javafx.scene.Parent;
//Là "gốc" của một giao diện. Khi nạp FXML, kết quả trả về thường là một Parent để bạn đưa vào Scene.
import javafx.scene.Scene;
//Đại diện cho một Cảnh phim. Một cửa sổ có thể có nhiều cảnh (Cảnh Đăng ký, Cảnh Chọn vai trò).import javafx.scene.control.TextField;
import javafx.scene.control.TextField;
//Để Java hiểu và điều khiển được ô nhập văn bản.
import javafx.stage.Stage;
//Đại diện cho cái Sân khấu (Cửa sổ ứng dụng). Bạn dùng nó để đổi tiêu đề hoặc thay đổi toàn bộ nội dung cửa sổ.
import java.io.IOException;
//Để xử lý tình huống "lỡ như" file FXML bị xóa hoặc sai tên, chương trình sẽ không bị sập mà báo lỗi một cách văn minh.
public class RegisterController {
    @FXML
    private TextField txtId; // ô nhập ID
    @FXML
    private TextField txtUsername; // ô nhập tên người dùng
    @FXML private PasswordField txtPassword;
    public static User currentUser;
    @FXML
    void handleRegister(ActionEvent event){
        String id = txtId.getText();
        String name = txtUsername.getText();
        String password = txtPassword.getText();
        if(id.isEmpty() || name.isEmpty() || password.isEmpty()){
            System.out.println("ko được để trống");
            return;
        }
        if (password.length() < 4){
            System.out.println("mật khẩu ko được ít hơn 4 từ");
            return;
        }
        currentUser = new Bidder(id, name, password, 0.0);
        System.out.println("đăng ký thành công: " + currentUser);

        try{
            switchToRoleSelection(event);
        } catch (IOException e) {
            System.err.println("lỗi: ko tìm thấy file role selection.fxml");
            e.printStackTrace();
        }
    }
    private void switchToRoleSelection(ActionEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("role-selection.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("HỆ THỐNG ĐẤU GIÁ-CHỌN VAI TRÒ");
        stage.show();
    }

}
