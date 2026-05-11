package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Đăng ký người dùng mới
    // ─────────────────────────────────────────────────────────────────────────
    public boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);  // TODO: nên hash password (BCrypt) trong production
            pstmt.setString(3, role);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser thất bại: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Đăng nhập – kiểm tra username + password (trả về true/false)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean loginUser(String username, String password) {
        return getRoleByLogin(username, password) != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Đăng nhập – trả về ROLE ("BIDDER" / "SELLER") hoặc null nếu sai
    // ─────────────────────────────────────────────────────────────────────────
    public String getRoleByLogin(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role"); // "BIDDER" hoặc "SELLER"
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getRoleByLogin thất bại: " + e.getMessage());
        }
        return null; // Sai tài khoản / mật khẩu
    }
}