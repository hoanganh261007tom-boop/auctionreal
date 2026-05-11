module org.example.auctionreal {
    // ── JavaFX ──────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;

    // ── JDBC + MySQL ─────────────────────────────────────────────────────────
    requires java.sql;           // BẮT BUỘC cho java.sql.*
        // BẮT BUỘC cho MySQL Connector/J 8.x

    // ── Mở package để JavaFX FXML có thể truy cập (reflection) ─────────────
    opens org.example.auctionreal to javafx.fxml;
    opens database to javafx.fxml;
    opens user to javafx.fxml;

    // ── Export để các package khác dùng được ────────────────────────────────
    exports org.example.auctionreal;
    exports database;
    exports user;
}