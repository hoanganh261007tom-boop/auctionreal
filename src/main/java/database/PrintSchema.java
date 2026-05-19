package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PrintSchema {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE users")) {
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getString(1)).append(" - ").append(rs.getString(2)).append("\n");
            }
            Files.write(Paths.get("schema_users.txt"), sb.toString().getBytes());
            System.out.println("Schema written to schema_users.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
