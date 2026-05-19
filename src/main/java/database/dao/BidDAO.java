package database.dao;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BidDAO {

    // =====================================================
    // PLACE BID
    // =====================================================
    public boolean placeBid(
            int auctionId,
            int bidderId,
            double bidAmount
    ) {

        String currentBidSql =
                "SELECT current_bid FROM auctions WHERE auction_id = ?";

        String insertBidSql =
                "INSERT INTO bids(auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";

        String updateAuctionSql =
                "UPDATE auctions SET current_bid = ? WHERE auction_id = ?";

        try (
                Connection conn =
                        DatabaseConnection.getConnection()
        ) {

            conn.setAutoCommit(false);

            // 1. GET CURRENT BID
            PreparedStatement currentStmt =
                    conn.prepareStatement(currentBidSql);

            currentStmt.setInt(1, auctionId);

            ResultSet rs =
                    currentStmt.executeQuery();

            double currentBid = 0;

            if (rs.next()) {

                currentBid =
                        rs.getDouble("current_bid");
            }

            // 2. VALIDATE
            if (bidAmount <= currentBid) {

                System.out.println(
                        "Bid phải lớn hơn giá hiện tại"
                );

                conn.rollback();

                return false;
            }

            // 3. INSERT BID
            PreparedStatement insertStmt =
                    conn.prepareStatement(insertBidSql);

            insertStmt.setInt(1, auctionId);

            insertStmt.setInt(2, bidderId);

            insertStmt.setDouble(3, bidAmount);

            insertStmt.executeUpdate();

            // 4. UPDATE AUCTION
            PreparedStatement updateStmt =
                    conn.prepareStatement(updateAuctionSql);

            updateStmt.setDouble(1, bidAmount);

            updateStmt.setInt(2, auctionId);

            updateStmt.executeUpdate();

            conn.commit();

            return true;

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }

    // =====================================================
    // GET CURRENT BID
    // =====================================================
    public double getCurrentBid(int auctionId) {

        String sql =
                "SELECT current_bid FROM auctions WHERE auction_id = ?";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, auctionId);

            ResultSet rs =
                    pstmt.executeQuery();

            if (rs.next()) {

                return rs.getDouble("current_bid");
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return 0;
    }

    // =====================================================
    // GET WINNER
    // =====================================================
    public String getWinner(int auctionId) {

        String sql =

                "SELECT users.username " +

                        "FROM bids " +

                        "JOIN users " +
                        "ON bids.bidder_id = users.user_id " +

                        "WHERE auction_id = ? " +

                        "ORDER BY bid_amount DESC " +

                        "LIMIT 1";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, auctionId);

            ResultSet rs =
                    pstmt.executeQuery();

            if (rs.next()) {

                return rs.getString("username");
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return "Không có người thắng";
    }

    // =====================================================
    // GET BID HISTORY
    // =====================================================
    public java.util.List<String> getBidHistory(
            int auctionId
    ) {

        java.util.List<String> history =
                new java.util.ArrayList<>();

        String sql =

                "SELECT users.username, bids.bid_amount " +

                        "FROM bids " +

                        "JOIN users " +

                        "ON bids.bidder_id = users.user_id " +

                        "WHERE auction_id = ? " +

                        "ORDER BY bids.bid_amount DESC";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, auctionId);

            ResultSet rs =
                    pstmt.executeQuery();

            while (rs.next()) {

                String username =
                        rs.getString("username");

                double amount =
                        rs.getDouble("bid_amount");

                String row =

                        "🔺 "
                                + username
                                + " → "
                                + amount
                                + " ₫";

                history.add(row);
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return history;
    }
}