package database.dao;

import database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // =====================================================
    // ADD ITEM
    // =====================================================
    public int addItem(String name, String description, double startPrice,
                       String category, String condition, int durationMins,
                       int ownerId, double minStep) {
        String sql = "INSERT INTO items(name, description, starting_price, current_price, " +
                "min_step, seller_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
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

    public int addItem(String name, String description, double startPrice,
                       String category, String condition, int durationMins, int ownerId) {
        return addItem(name, description, startPrice, category, condition, durationMins, ownerId, 1_000_000.0);
    }

    // =====================================================
    // UPDATE ITEM – Sửa sản phẩm
    // =====================================================
    public boolean updateItem(int itemId, String name, String description,
                              double startPrice, double minStep) {
        // Cập nhật items: name, description, starting_price, current_price, min_step
        // current_price cũng phải cập nhật vì chưa có ai đặt giá
        String sqlItem = "UPDATE items SET name = ?, description = ?, " +
                "starting_price = ?, current_price = ?, min_step = ? WHERE item_id = ?";
        // Đồng bộ current_bid trong auctions (chỉ khi chưa ai đặt giá)
        String sqlAuction = "UPDATE auctions SET current_bid = ? " +
                "WHERE item_id = ? AND current_bid <= " +
                "(SELECT starting_price FROM items WHERE item_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Cập nhật bảng items
                PreparedStatement ps1 = conn.prepareStatement(sqlItem);
                ps1.setString(1, name);
                ps1.setString(2, description);
                ps1.setDouble(3, startPrice);
                ps1.setDouble(4, startPrice); // current_price = starting_price khi chưa ai đặt
                ps1.setDouble(5, minStep);
                ps1.setInt(6, itemId);
                int rows = ps1.executeUpdate();

                // 2. Đồng bộ current_bid trong auctions
                PreparedStatement ps2 = conn.prepareStatement(sqlAuction);
                ps2.setDouble(1, startPrice);
                ps2.setInt(2, itemId);
                ps2.setInt(3, itemId);
                ps2.executeUpdate();

                conn.commit();
                return rows > 0;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // DELETE ITEM – Xóa sản phẩm
    // =====================================================
    public boolean deleteItem(int itemId) {
        // Xóa theo thứ tự: bids → auctions → items (tránh lỗi foreign key)
        String deleteBidsSql    = "DELETE FROM bids WHERE auction_id IN " +
                "(SELECT auction_id FROM auctions WHERE item_id = ?)";
        String deleteAuctionSql = "DELETE FROM auctions WHERE item_id = ?";
        String deleteItemSql    = "DELETE FROM items WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Xóa bids liên quan
                PreparedStatement s1 = conn.prepareStatement(deleteBidsSql);
                s1.setInt(1, itemId);
                s1.executeUpdate();

                // 2. Xóa auctions liên quan
                PreparedStatement s2 = conn.prepareStatement(deleteAuctionSql);
                s2.setInt(1, itemId);
                s2.executeUpdate();

                // 3. Xóa item
                PreparedStatement s3 = conn.prepareStatement(deleteItemSql);
                s3.setInt(1, itemId);
                int rows = s3.executeUpdate();

                conn.commit();
                return rows > 0;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // GET ITEMS BY SELLER – Lấy danh sách item của seller
    // =====================================================
    public List<int[]> getItemIdsBySeller(int sellerId) {
        // Trả về list [item_id] của seller
        List<int[]> ids = new ArrayList<>();
        String sql = "SELECT item_id FROM items WHERE seller_id = ? ORDER BY item_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sellerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ids.add(new int[]{rs.getInt("item_id")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    // =====================================================
    // GET ALL AUCTION ITEMS – Dùng cho BidderDashboard
    // =====================================================
    public List<AuctionItemInfo> getAllAuctionItems() {
        List<AuctionItemInfo> list = new ArrayList<>();
        String sql = "SELECT i.item_id, i.name, i.description, i.starting_price, " +
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
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // GET ITEMS BY SELLER – Dùng cho SellerDashboard
    // =====================================================
    public List<AuctionItemInfo> getItemsBySeller(int sellerId) {
        List<AuctionItemInfo> list = new ArrayList<>();
        String sql = "SELECT i.item_id, i.name, i.description, i.starting_price, " +
                "i.current_price, i.min_step, i.status, " +
                "COALESCE(a.auction_id, -1) AS auction_id " +
                "FROM items i " +
                "LEFT JOIN auctions a ON a.item_id = i.item_id " +
                "WHERE i.seller_id = ? ORDER BY i.item_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sellerId);
            ResultSet rs = pstmt.executeQuery();
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
                info.sellerName   = "Bạn";
                list.add(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getLastInsertedItemId() {
        String sql = "SELECT MAX(item_id) AS latest_id FROM items";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt("latest_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // =====================================================
    // DATA CLASS
    // =====================================================
    public static class AuctionItemInfo {
        public int    itemId;
        public int    auctionId;
        public String name;
        public String description;
        public double startPrice;
        public double currentPrice;
        public double minStep;
        public String status;
        public String sellerName;
    }
}
