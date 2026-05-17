module org.example.auctionreal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens org.example.auctionreal to javafx.fxml;
    exports org.example.auctionreal;

    opens database to javafx.fxml;
    exports database;

    opens user to javafx.fxml;
    exports user;
}