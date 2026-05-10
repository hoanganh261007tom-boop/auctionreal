package org.example.auctionreal;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    // Hàm lấy tất cả sản phẩm đang đấu giá
    public List<String> getAllItems() {
        List<String> items = new ArrayList<>();
        String sql = "SELECT name FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}