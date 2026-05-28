package database.dao;

import database.DatabaseConnection;

import java.sql.*;
import java.sql.Timestamp;

public class AuctionDAO {

    // CLOSE AUCTION
    public boolean closeAuction(int auctionId) {

        String sql =
                "UPDATE auctions " +
                        "SET status = 'CLOSED' " +
                        "WHERE auction_id = ?";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, auctionId);

            int rows =
                    pstmt.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }
    public boolean createAuction(

            int itemId,
            Timestamp startTime,
            Timestamp endTime,
            double startPrice

    ) {

        String sql =

                "INSERT INTO auctions(" +

                        "item_id, " +
                        "start_time, " +
                        "end_time, " +
                        "current_bid, " +
                        "status" +

                        ") VALUES (?, ?, ?, ?, ?)";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, itemId);

            pstmt.setTimestamp(2, startTime);

            pstmt.setTimestamp(3, endTime);

            pstmt.setDouble(4, startPrice);

            pstmt.setString(5, "OPEN");

            int rows =
                    pstmt.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }

    // =====================================================
    // GET REMAINING SECONDS – Tính số giây còn lại từ end_time
    // =====================================================

    /**
     * Trả về số giây còn lại cho phiên đấu giá.
     * Nếu phiên đã kết thúc hoặc không tìm thấy → trả về 0.
     */
    public int getRemainingSeconds(int auctionId) {
        String sql = "SELECT end_time FROM auctions WHERE auction_id = ? AND status = 'OPEN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp endTime = rs.getTimestamp("end_time");
                if (endTime != null) {
                    long remainingMs = endTime.getTime() - System.currentTimeMillis();
                    return Math.max(0, (int) (remainingMs / 1000));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Cập nhật end_time khi anti-sniping gia hạn thêm giây.
     */
    public boolean extendEndTime(int auctionId, int extraSeconds) {
        String sql = "UPDATE auctions SET end_time = DATE_ADD(end_time, INTERVAL ? SECOND) " +
                     "WHERE auction_id = ? AND status = 'OPEN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, extraSeconds);
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}