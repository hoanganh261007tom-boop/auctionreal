package org.example.auctionreal;

import java.io.*;
import java.net.*;

public class SimpleAuctionServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("🚀 Server giả lập đang chạy ở cổng 1234...");
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("✅ Đã kết nối với App của Cloud!");
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("📩 Nhận yêu cầu: " + inputLine);
                        // Giả lập Server xử lý xong và gửi thông báo NEW_BID về cho tất cả mọi người
                        if (inputLine.startsWith("PLACE_BID")) {
                            String[] parts = inputLine.split("\\|");
                            String response = "NEW_BID|" + parts[1] + "|" + parts[2];
                            out.println(response);
                            System.out.println("📢 Đã phản hồi: " + response);
                        }
                    }
                } catch (IOException e) { System.out.println("Ngắt kết nối với 1 client."); }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}