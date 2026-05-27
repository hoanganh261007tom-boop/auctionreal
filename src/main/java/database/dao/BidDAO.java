package database.dao;

import database.DatabaseConnection;
import org.example.auctionreal.observer.AuctionEventManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BidDAO – Xử lý đặt giá với bảo vệ Concurrent Bidding.
 *
 * Vấn đề: Nếu 2 người đặt giá CÙNG LÚC, cả 2 có thể đọc
 * cùng 1 current_bid và cùng "thắng" → Lost update / Race condition.
 *
 * Giải pháp: synchronized + SELECT ... FOR UPDATE (DB-level lock)
 * → Chỉ 1 người được xử lý tại 1 thời điểm cho mỗi phiên đấu giá.
 */
public class BidDAO {

    /**
     * Lock object per auction để tránh block các phiên khác nhau.
     * Dùng String.intern() để các thread cùng auctionId dùng chung lock.
     */
    private static Object getLock(int auctionId) {
        return ("auction_lock_" + auctionId).intern();
    }

    // =====================================================
    // PLACE BID – Thread-safe với synchronized + DB transaction
    // =====================================================
    public boolean placeBid(int auctionId, int bidderId, double bidAmount) {

        // synchronized theo từng auctionId → các phiên khác không bị block
        synchronized (getLock(auctionId)) {

            String selectSql = "SELECT current_bid FROM auctions " +
                    "WHERE auction_id = ? AND status = 'OPEN' FOR UPDATE";
            String insertSql = "INSERT INTO bids(auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
            String updateSql = "UPDATE auctions SET current_bid = ? WHERE auction_id = ?";
            // Cũng cập nhật current_price trong items
            String updateItemSql = "UPDATE items SET current_price = ? " +
                    "WHERE item_id = (SELECT item_id FROM auctions WHERE auction_id = ?)";

            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false); // Bắt đầu transaction

                // 1. SELECT FOR UPDATE – lock row tại DB level
                PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                selectStmt.setInt(1, auctionId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    // Phiên không tồn tại hoặc đã đóng
                    conn.rollback();
                    System.out.println("[BidDAO] ❌ Phiên #" + auctionId + " không tồn tại hoặc đã đóng.");
                    return false;
                }

                double currentBid = rs.getDouble("current_bid");

                // 2. VALIDATE – giá phải cao hơn hiện tại
                if (bidAmount <= currentBid) {
                    conn.rollback();
                    System.out.println("[BidDAO] ❌ Bid " + bidAmount + " ≤ giá hiện tại " + currentBid);
                    return false;
                }

                // 3. INSERT BID
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, auctionId);
                insertStmt.setInt(2, bidderId);
                insertStmt.setDouble(3, bidAmount);
                insertStmt.executeUpdate();

                // 4. UPDATE auctions.current_bid
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setDouble(1, bidAmount);
                updateStmt.setInt(2, auctionId);
                updateStmt.executeUpdate();

                // 5. UPDATE items.current_price (đồng bộ giá)
                PreparedStatement updateItemStmt = conn.prepareStatement(updateItemSql);
                updateItemStmt.setDouble(1, bidAmount);
                updateItemStmt.setInt(2, auctionId);
                updateItemStmt.executeUpdate();

                conn.commit(); // COMMIT transaction

                System.out.println("[BidDAO] ✅ Bid thành công: " + bidAmount
                        + "₫ bởi bidder #" + bidderId + " (phiên #" + auctionId + ")");

                // 6. Thông báo Observer Pattern
                String winner = getWinner(auctionId);
                AuctionEventManager.getInstance()
                        .notifyBidUpdated(auctionId, bidAmount, winner);

                return true;

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
                System.err.println("[BidDAO] ❌ Lỗi SQL placeBid: " + e.getMessage());
                return false;
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        } // end synchronized
    }

    // =====================================================
    // GET CURRENT BID
    // =====================================================
    public double getCurrentBid(int auctionId) {
        String sql = "SELECT current_bid FROM auctions WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("current_bid");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =====================================================
    // GET WINNER
    // =====================================================
    public String getWinner(int auctionId) {
        String sql = "SELECT users.username FROM bids " +
                "JOIN users ON bids.bidder_id = users.user_id " +
                "WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Không có người thắng";
    }

    // =====================================================
    // GET BID HISTORY
    // =====================================================
    public List<String> getBidHistory(int auctionId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT users.username, bids.bid_amount, bids.bid_time " +
                "FROM bids JOIN users ON bids.bidder_id = users.user_id " +
                "WHERE auction_id = ? ORDER BY bids.bid_amount DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            java.text.NumberFormat fmt =
                    java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
            while (rs.next()) {
                String username = rs.getString("username");
                double amount   = rs.getDouble("bid_amount");
                history.add("🔺 " + username + " → " + fmt.format((long)amount) + " ₫");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}
