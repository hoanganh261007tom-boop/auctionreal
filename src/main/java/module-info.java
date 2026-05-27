module org.example.auctionreal {
    // ── JavaFX ──────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;

    // ── JDBC + MySQL ─────────────────────────────────────────────────────────
    requires java.sql;

    // ── Mở package để JavaFX FXML có thể truy cập (reflection) ─────────────
    opens org.example.auctionreal to javafx.fxml;
    opens database to javafx.fxml;
    opens user to javafx.fxml;
    opens model to javafx.fxml;

    // ── Export để các package khác dùng được ────────────────────────────────
    exports org.example.auctionreal;
    exports org.example.auctionreal.network;
    exports org.example.auctionreal.observer;
    exports database;
    exports user;
    exports database.model;
    exports database.dao;
    opens database.dao to javafx.fxml;
    opens org.example.auctionreal.network to javafx.fxml;
    opens org.example.auctionreal.observer to javafx.fxml;
}
