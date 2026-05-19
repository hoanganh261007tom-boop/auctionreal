package database.dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO – Truy vấn dữ liệu vật phẩm đấu giá từ MySQL.
 * Đặt trong package org.example.auctionreal để các Controller dùng trực tiếp.
 */
public class ItemDAO {

    /**
     * Lấy danh sách tất cả vật phẩm đang đấu giá (đơn giản: chỉ tên).
     * Dùng cho danh sách nhanh trong ListView.
     */
    public List<String> getAllItemNames() {
        List<String> items = new ArrayList<>();
        String sql = "SELECT name FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getAllItemNames thất bại: " + e.getMessage());
        }
        return items;
    }

    /**
     * Lấy danh sách vật phẩm đầy đủ thông tin để hiển thị trong dashboard.
     * Mỗi phần tử là chuỗi định dạng: "name | start_price ₫ | seller | status"
     */
    public List<String> getAllItemsFormatted() {
        List<String> items = new ArrayList<>();
        // Join với bảng users để lấy tên người bán
        String sql = "SELECT i.item_id, " +
                "i.name, " +
                "i.starting_price, " +
                "i.status, " +
                "COALESCE(u.username, 'Ẩn danh') AS seller_name " +
                "FROM items i " +
                "LEFT JOIN users u " +
                "ON i.seller_id = u.user_id " +
                "ORDER BY i.item_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            java.text.NumberFormat fmt =
                    java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));

            while (rs.next()) {
                String name   = rs.getString("name");
                double price  = rs.getDouble("starting_price");
                String seller = rs.getString("seller_name");
                String status = rs.getString("status");
                if (status == null) status = "Đang đấu giá";

                String line = String.format("🏷 %s  |  %s ₫  |  👤 %s  |  %s",
                        name, fmt.format((long) price), seller, status);
                items.add(line);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getAllItemsFormatted thất bại: " + e.getMessage());
        }
        return items;
    }

    /**
     * Thêm vật phẩm mới vào DB (dùng cho SellerDashboard).
     */
    public int addItem(
            String name,
            String description,
            double startPrice,
            String category,
            String condition,
            int durationMins,
            int ownerId
    ) {
        String sql =
                "INSERT INTO items(" +
                        "name, " +
                        "description, " +
                        "starting_price, " +
                        "current_price, " +
                        "seller_id, " +
                        "status" +
                        ") VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, startPrice);
            pstmt.setDouble(4, startPrice);
            pstmt.setInt(5, ownerId);
            pstmt.setString(6, "OPEN");

            int rows =
                    pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs =
                        pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("error.log"), e.getMessage().getBytes());
            } catch (Exception ex) {}
            e.printStackTrace();
        }

        return -1;
    }
    public int getLastInsertedItemId() {

        String sql =
                "SELECT MAX(item_id) AS latest_id FROM items";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql);

                ResultSet rs =
                        pstmt.executeQuery()

        ) {

            if (rs.next()) {

                return rs.getInt("latest_id");
            }

        } catch (SQLException e) {

            System.out.println(
                    e.getMessage()
            );
        }

        return -1;
    }
}