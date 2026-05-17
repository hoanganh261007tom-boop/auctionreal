package database.dao;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
}