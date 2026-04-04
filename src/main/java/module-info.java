module org.example.auctionreal {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens org.example.auctionreal to javafx.fxml;
    exports org.example.auctionreal;
}