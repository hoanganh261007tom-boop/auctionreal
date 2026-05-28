package org.example.auctionreal.network;

import database.dao.AuctionDAO;
import database.dao.BidDAO;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler: Xử lý kết nối từ một client cụ thể.
 *
 * Protocol (text-based, mỗi message = 1 dòng):
 *
 * Client → Server:
 *   JOIN:<auctionId>:<userId>:<username>
 *   BID:<auctionId>:<bidderId>:<amount>
 *   LEAVE
 *   PING
 *
 * Server → Client:
 *   BID_UPDATE:<auctionId>:<currentPrice>:<topBidder>:<bidCount>
 *   AUCTION_CLOSED:<auctionId>:<winner>
 *   TIME_SYNC:<auctionId>:<remainingSeconds>       ★ Đồng bộ đồng hồ
 *   ANTI_SNIPE:<auctionId>:<extraSeconds>           ★ Thông báo gia hạn
 *   USER_JOINED:<auctionId>:<username>
 *   USER_LEFT:<auctionId>:<username>
 *   PONG
 *   ERROR:<message>
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = true;

    /** Phiên đấu giá mà client đang theo dõi (-1 = chưa join). */
    private int watchingAuctionId = -1;

    /** Tên người dùng (được set khi JOIN). */
    private String username = "Unknown";

    /** User ID (được set khi JOIN). */
    private int userId = -1;

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while (connected && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                System.out.println("[ClientHandler] 📩 Nhận: " + line);
                processMessage(line);
            }

        } catch (IOException e) {
            if (connected) {
                System.out.println("[ClientHandler] 🔌 Client ngắt kết nối: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    // =====================================================
    // XỬ LÝ MESSAGE
    // =====================================================

    private void processMessage(String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "PING" -> handlePing();
            case "JOIN" -> handleJoin(message);
            case "BID" -> handleBid(message);
            case "LEAVE" -> handleLeave();
            default -> sendMessage("ERROR:Lệnh không hợp lệ: " + command);
        }
    }

    // ─────────────────────────────
    // PING
    // ─────────────────────────────
    private void handlePing() {
        sendMessage("PONG");
    }

    // ─────────────────────────────
    // JOIN:<auctionId>:<userId>:<username>
    // ─────────────────────────────
    private void handleJoin(String message) {
        String[] parts = message.split(":");
        if (parts.length < 4) {
            sendMessage("ERROR:Format sai. Dùng JOIN:<auctionId>:<userId>:<username>");
            return;
        }

        try {
            this.watchingAuctionId = Integer.parseInt(parts[1]);
            this.userId = Integer.parseInt(parts[2]);
            this.username = parts[3];
        } catch (NumberFormatException e) {
            sendMessage("ERROR:auctionId và userId phải là số nguyên");
            return;
        }

        System.out.println("[ClientHandler] ✅ " + username + " đã tham gia phiên #" + watchingAuctionId);

        // Thông báo cho các client khác cùng phiên
        server.broadcastToAuction(watchingAuctionId,
                "USER_JOINED:" + watchingAuctionId + ":" + username);

        // Gửi trạng thái hiện tại cho client vừa join
        BidDAO bidDAO = new BidDAO();
        double currentBid = bidDAO.getCurrentBid(watchingAuctionId);
        String topBidder = bidDAO.getWinner(watchingAuctionId);
        sendMessage("BID_UPDATE:" + watchingAuctionId + ":" + currentBid + ":" + topBidder + ":0");

        // ★ GỬI TIME_SYNC ngay lập tức để client có đúng thời gian còn lại
        AuctionDAO auctionDAO = new AuctionDAO();
        int remaining = auctionDAO.getRemainingSeconds(watchingAuctionId);
        sendMessage("TIME_SYNC:" + watchingAuctionId + ":" + remaining);
        System.out.println("[ClientHandler] ⏱ Gửi TIME_SYNC cho " + username
                + ": " + remaining + " giây còn lại.");
    }

    // ─────────────────────────────
    // BID:<auctionId>:<bidderId>:<amount>
    // ─────────────────────────────
    private void handleBid(String message) {
        String[] parts = message.split(":");
        if (parts.length < 4) {
            sendMessage("ERROR:Format sai. Dùng BID:<auctionId>:<bidderId>:<amount>");
            return;
        }

        int auctionId;
        int bidderId;
        double amount;

        try {
            auctionId = Integer.parseInt(parts[1]);
            bidderId = Integer.parseInt(parts[2]);
            amount = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            sendMessage("ERROR:Dữ liệu bid không hợp lệ");
            return;
        }

        // Gọi DAO để lưu bid vào database
        BidDAO bidDAO = new BidDAO();
        boolean success = bidDAO.placeBid(auctionId, bidderId, amount);

        if (success) {
            System.out.println("[ClientHandler] 💰 Bid thành công: " + username
                    + " → " + amount + " ₫ (phiên #" + auctionId + ")");

            // Broadcast BID_UPDATE cho tất cả client theo dõi phiên này
            String topBidder = bidDAO.getWinner(auctionId);
            server.broadcastToAuction(auctionId,
                    "BID_UPDATE:" + auctionId + ":" + amount + ":" + topBidder + ":1");

            // ★ ANTI-SNIPING phía server: nếu bid trong 30 giây cuối → gia hạn
            AuctionDAO auctionDAO = new AuctionDAO();
            int remaining = auctionDAO.getRemainingSeconds(auctionId);
            if (remaining > 0 && remaining <= 30) {
                boolean extended = auctionDAO.extendEndTime(auctionId, 60);
                if (extended) {
                    int newRemaining = auctionDAO.getRemainingSeconds(auctionId);
                    server.broadcastToAuction(auctionId,
                            "TIME_SYNC:" + auctionId + ":" + newRemaining);
                    server.broadcastToAuction(auctionId,
                            "ANTI_SNIPE:" + auctionId + ":60");
                    System.out.println("[ClientHandler] ⏱ Anti-sniping: phiên #"
                            + auctionId + " gia hạn +60s. Còn lại: " + newRemaining + "s");
                }
            }

        } else {
            sendMessage("ERROR:Đặt giá thất bại! Giá phải cao hơn giá hiện tại.");
        }
    }

    // ─────────────────────────────
    // LEAVE
    // ─────────────────────────────
    private void handleLeave() {
        if (watchingAuctionId > 0) {
            server.broadcastToAuction(watchingAuctionId,
                    "USER_LEFT:" + watchingAuctionId + ":" + username);
            System.out.println("[ClientHandler] 👋 " + username + " rời phiên #" + watchingAuctionId);
        }
        watchingAuctionId = -1;
    }

    // =====================================================
    // GỬI MESSAGE
    // =====================================================

    /**
     * Gửi message tới client này.
     */
    public void sendMessage(String message) {
        if (writer != null && connected) {
            writer.println(message);
            writer.flush();
        }
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    /**
     * Đóng kết nối và dọn dẹp tài nguyên.
     */
    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void cleanup() {
        connected = false;
        if (watchingAuctionId > 0) {
            server.broadcastToAuction(watchingAuctionId,
                    "USER_LEFT:" + watchingAuctionId + ":" + username);
        }
        server.removeClient(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public int getWatchingAuctionId() {
        return watchingAuctionId;
    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }
}
