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

    // Hàm khởi tạo nhận vào Controller để có thể gọi hàm cập nhật giao diện
    public AuctionSocketClient(AuctionController controller) {
        this.controller = controller;
    }

    // Hàm kết nối đến Server
    public void connectToServer(String host, int port) {
        try {
            System.out.println("Đang thử kết nối đến Server tại " + host + ":" + port + "...");
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("✅ Kết nối Server thành công!");

            // Tạo luồng riêng biệt để liên tục lắng nghe tin nhắn từ Server
            listenerThread = new Thread(() -> {
                try {
                    String message;
                    // Vòng lặp chờ tin nhắn tới
                    while ((message = in.readLine()) != null) {
                        System.out.println("📥 Nhận được từ Server: " + message);

                        // Kiểm tra định dạng tin nhắn quy ước: NEW_BID|TênNgườiĐặt|SốTiền
                        if (message.startsWith("NEW_BID")) {
                            String[] parts = message.split("\\|");
                            if (parts.length == 3) {
                                String bidderName = parts[1];
                                double amount = Double.parseDouble(parts[2]);

                                // BẮT BUỘC: Đưa lệnh cập nhật UI vào JavaFX Application Thread
                                Platform.runLater(() -> {
                                    controller.applyNewBidToUI(amount, bidderName);
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("🔌 Đã ngắt kết nối với Server.");
                }
            });

            // Đảm bảo luồng này tự động tắt khi bạn tắt ứng dụng
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("❌ LỖI: Không thể kết nối đến Server. Vui lòng bảo Backend kiểm tra xem Server đã bật chưa!");
        }
    }

    // Hàm gửi mức giá mới lên Server
    public void sendBid(String bidderName, double amount) {
        if (out != null && !socket.isClosed()) {
            String msg = "PLACE_BID|" + bidderName + "|" + amount;
            out.println(msg);
            System.out.println("📤 Đã gửi lên Server: " + msg);
        } else {
            System.err.println("❌ LỖI: Chưa kết nối được với Server, không thể gửi giá!");
        }
    }

    // Hàm ngắt kết nối an toàn khi người dùng thoát phòng đấu giá
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Đã đóng Socket Client thành công.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}