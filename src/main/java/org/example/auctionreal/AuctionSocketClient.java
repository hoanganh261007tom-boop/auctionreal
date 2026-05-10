package org.example.auctionreal;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionSocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private AuctionController controller;
    private Thread listenerThread;

    public AuctionSocketClient(AuctionController controller) {
        this.controller = controller;
    }

    public void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            listenerThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Giả sử Server gửi về chuỗi: "NEW_BID|TênNgườiĐặt|SốTiền"
                        if (message.startsWith("NEW_BID")) {
                            String[] parts = message.split("\\|");
                            if (parts.length == 3) {
                                String bidderName = parts[1];
                                double amount = Double.parseDouble(parts[2]);

                                // BẮT BUỘC dùng Platform.runLater để luồng phụ cập nhật UI chính
                                Platform.runLater(() -> {
                                    controller.applyNewBidToUI(amount, bidderName);
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Mất kết nối với Server.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.out.println("Không thể kết nối đến Server đấu giá: " + e.getMessage());
        }
    }

    public void sendBid(String bidderName, double amount) {
        if (out != null) {
            // Gửi dữ liệu lên Server (Người 1 sẽ viết code hứng chuỗi này)
            out.println("PLACE_BID|" + bidderName + "|" + amount);
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}