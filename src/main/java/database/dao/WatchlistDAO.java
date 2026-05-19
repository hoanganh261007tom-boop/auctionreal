package database.dao;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WatchlistDAO {

    public boolean addToWatchlist(

            int userId,
            int auctionId
    ) {

        String sql =

                "INSERT INTO watchlist(" +
                        "user_id, auction_id" +
                        ") VALUES (?, ?)";

        try (

                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)

        ) {

            pstmt.setInt(1, userId);

            pstmt.setInt(2, auctionId);

            int rows =
                    pstmt.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }
}