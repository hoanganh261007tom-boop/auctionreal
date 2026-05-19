package database;

import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Connected!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // Đóng hàm main
} // Đóng class TestConnection (Cái này thường bị thiếu)

