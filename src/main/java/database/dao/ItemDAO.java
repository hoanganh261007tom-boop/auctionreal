package database.dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    /**
     * Lấy danh sách tên vật phẩm đơn giản.
     */
    public List<String> getAllItemNames() {
        List<String> items = new ArrayList<>();
        String sql = "SELECT name FROM items";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getAllItemNames thất bại: " + e.getMessage());
        }
        return items;
    }

    /**
     * Lấy danh sách vật phẩm đầy đủ thông tin (Item objects).
     * Join với bảng auctions để lấy auction_id thật.
     */


    /**
     * Lấy danh sách vật phẩm kèm auction_id — dành riêng cho BidderDashboard.
     */
    public List<AuctionItemInfo> getAllAuctionItems() {
        List<AuctionItemInfo> list = new ArrayList<>();
        String sql =
                "SELECT i.item_id, i.name, i.description, i.starting_price, " +
                        "i.current_price, i.min_step, i.status, " +
                        "COALESCE(u.username, 'Ẩn danh') AS seller_name, " +
                        "COALESCE(a.auction_id, -1) AS auction_id " +
                        "FROM items i " +
                        "LEFT JOIN users u ON i.seller_id = u.user_id " +
                        "LEFT JOIN auctions a ON a.item_id = i.item_id AND a.status = 'OPEN' " +
                        "ORDER BY i.item_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                AuctionItemInfo info = new AuctionItemInfo();
                info.itemId       = rs.getInt("item_id");
                info.auctionId    = rs.getInt("auction_id");
                info.name         = rs.getString("name");
                info.description  = rs.getString("description");
                info.startPrice   = rs.getDouble("starting_price");
                info.currentPrice = rs.getDouble("current_price");
                info.minStep      = rs.getDouble("min_step");
                info.status       = rs.getString("status");
                info.sellerName   = rs.getString("seller_name");
                list.add(info);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getAllAuctionItems thất bại: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách vật phẩm định dạng chuỗi (giữ lại cho tương thích ngược).
     */
    public List<String> getAllItemsFormatted() {
        List<String> items = new ArrayList<>();
        String sql =
                "SELECT i.item_id, i.name, i.starting_price, i.status, " +
                        "COALESCE(u.username, 'Ẩn danh') AS seller_name " +
                        "FROM items i " +
                        "LEFT JOIN users u ON i.seller_id = u.user_id " +
                        "ORDER BY i.item_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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
     * Lưu cả min_step.
     */
    public int addItem(
            String name,
            String description,
            double startPrice,
            String category,
            String condition,
            int durationMins,
            int ownerId,
            double minStep
    ) {
        String sql =
                "INSERT INTO items(" +
                        "name, description, starting_price, current_price, min_step, seller_id, status" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, startPrice);
            pstmt.setDouble(4, startPrice);
            pstmt.setDouble(5, minStep);
            pstmt.setInt(6, ownerId);
            pstmt.setString(7, "OPEN");

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Giữ lại method cũ (6 tham số) cho tương thích — dùng minStep mặc định 1 triệu.
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
        return addItem(name, description, startPrice, category, condition, durationMins, ownerId, 1_000_000.0);
    }

    public int getLastInsertedItemId() {
        String sql = "SELECT MAX(item_id) AS latest_id FROM items";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt("latest_id");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    /**
     * Data class chứa đầy đủ thông tin item + auction_id để BidderDashboard dùng.
     */
    public static class AuctionItemInfo {
        public int itemId;
        public int auctionId;   // auction_id thật từ bảng auctions (-1 nếu chưa có)
        public String name;
        public String description;
        public double startPrice;
        public double currentPrice;
        public double minStep;
        public String status;
        public String sellerName;
    }
}
