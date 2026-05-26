package org.example.auctionreal;

import javafx.application.Application;
import org.example.auctionreal.network.AuctionServer;

public class Launcher {

    /** Server socket instance – chia sẻ để các controller truy cập được. */
    public static AuctionServer auctionServer;

    public static void main(String[] args) {
        // ── Khởi động AuctionServer trước khi chạy JavaFX ──
        auctionServer = new AuctionServer(9999);
        auctionServer.start();

        // Shutdown hook: tắt server khi đóng app
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (auctionServer != null) {
                auctionServer.stop();
            }
        }));

        // ── Chạy JavaFX ──
        Application.launch(HelloApplication.class, args);
    }
}
