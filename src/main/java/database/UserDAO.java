package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // Inner class chứa thông tin đăng nhập đầy đủ
    // ─────────────────────────────────────────────────────────────────────────
    public static class LoginResult {
        public final int    id;
        public final String role;
        public final double balance;

        public LoginResult(int id, String role, double balance) {
            this.id      = id;
            this.role    = role;
            this.balance = balance;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Đăng ký người dùng mới
    // ─────────────────────────────────────────────────────────────────────────
    public int registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);  // TODO: nên hash password (BCrypt) trong production
            pstmt.setString(3, role);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser thất bại: " + e.getMessage());
        }
        return -1;
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
        LoginResult result = getUserByLogin(username, password);
        return result != null ? result.role : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Đăng nhập đầy đủ – trả về LoginResult (id + role + balance) hoặc null
    // ─────────────────────────────────────────────────────────────────────────
    public LoginResult getUserByLogin(String username, String password) {
        // Thử lấy cả balance nếu có cột đó, ngược lại fallback về 0
        String sql = "SELECT id, role FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int    id      = rs.getInt("id");
                String role    = rs.getString("role");
                double balance = 0.0;
                // Thử đọc cột balance nếu tồn tại trong bảng
                try { balance = rs.getDouble("balance"); } catch (SQLException ignored) {}
                return new LoginResult(id, role, balance);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserByLogin thất bại: " + e.getMessage());
        }
        return null;
    }
}