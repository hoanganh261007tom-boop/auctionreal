package org.example.auctionreal.network;

import database.dao.AuctionDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AuctionServer: TCP Server cho hệ thống đấu giá real-time.
 * Lắng nghe kết nối từ các client (JavaFX app) và broadcast
 * các sự kiện đấu giá (bid, close, join) tới tất cả client.
 *
 * ĐẶC BIỆT: Broadcast TIME_SYNC mỗi giây để đồng bộ đồng hồ
 * đếm ngược giữa tất cả client. Thời gian được lấy từ end_time
 * trong database → tất cả client luôn hiển thị cùng một thời gian.
 */
public class AuctionServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;

    /** Danh sách tất cả client đang kết nối (thread-safe). */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** Scheduler để broadcast TIME_SYNC định kỳ. */
    private ScheduledExecutorService timeSyncScheduler;

    public AuctionServer(int port) {
        this.port = port;
    }

    // =====================================================
    // START / STOP
    // =====================================================

    /**
     * Khởi động server trong thread riêng.
     * Không block thread gọi.
     */
    public void start() {
        if (running) return;

        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                System.out.println("[AuctionServer] ✅ Đang lắng nghe trên port " + port + "...");

                // Khởi động TIME_SYNC scheduler
                startTimeSyncBroadcast();

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[AuctionServer] 🔗 Client mới kết nối: "
                                + clientSocket.getRemoteSocketAddress());

                        ClientHandler handler = new ClientHandler(clientSocket, this);
                        clients.add(handler);
                        new Thread(handler, "ClientHandler-" + clients.size()).start();

                    } catch (IOException e) {
                        if (running) {
                            System.err.println("[AuctionServer] ❌ Lỗi accept: " + e.getMessage());
                        }
                        // Nếu !running → server đang shutdown, bỏ qua lỗi
                    }
                }

            } catch (IOException e) {
                System.err.println("[AuctionServer] ❌ Không thể khởi động server trên port "
                        + port + ": " + e.getMessage());
            }
        }, "AuctionServer-Accept");

        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Dừng server: đóng tất cả client, đóng ServerSocket, dừng scheduler.
     */
    public void stop() {
        running = false;
        System.out.println("[AuctionServer] 🛑 Đang tắt server...");

        // Dừng TIME_SYNC scheduler
        if (timeSyncScheduler != null && !timeSyncScheduler.isShutdown()) {
            timeSyncScheduler.shutdownNow();
        }

        // Đóng tất cả client handler
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();

        // Đóng ServerSocket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[AuctionServer] Lỗi khi đóng ServerSocket: " + e.getMessage());
        }

        System.out.println("[AuctionServer] ✅ Server đã tắt.");
    }

    // =====================================================
    // TIME SYNC – Đồng bộ đồng hồ đếm ngược
    // =====================================================

    /**
     * Khởi động scheduler broadcast TIME_SYNC mỗi giây.
     * Lấy remaining seconds từ DB (end_time) và gửi cho tất cả client.
     */
    private void startTimeSyncBroadcast() {
        timeSyncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TimeSyncBroadcaster");
            t.setDaemon(true);
            return t;
        });

        timeSyncScheduler.scheduleAtFixedRate(() -> {
            try {
                if (clients.isEmpty()) return;

                // Tìm tất cả auctionId đang được theo dõi
                Set<Integer> activeAuctionIds = new HashSet<>();
                for (ClientHandler client : clients) {
                    int aid = client.getWatchingAuctionId();
                    if (aid > 0) activeAuctionIds.add(aid);
                }

                if (activeAuctionIds.isEmpty()) return;

                // Query remaining seconds cho mỗi auction và broadcast
                AuctionDAO auctionDAO = new AuctionDAO();
                for (int auctionId : activeAuctionIds) {
                    int remaining = auctionDAO.getRemainingSeconds(auctionId);
                    broadcastToAuction(auctionId,
                            "TIME_SYNC:" + auctionId + ":" + remaining);

                    // Nếu hết giờ → đóng phiên tự động
                    if (remaining <= 0) {
                        auctionDAO.closeAuction(auctionId);
                        database.dao.BidDAO bidDAO = new database.dao.BidDAO();
                        String winner = bidDAO.getWinner(auctionId);
                        broadcastToAuction(auctionId,
                                "AUCTION_CLOSED:" + auctionId + ":" + winner);
                        System.out.println("[AuctionServer] ⏰ Phiên #" + auctionId
                                + " hết giờ → đóng. Winner: " + winner);
                    }
                }
            } catch (Exception e) {
                System.err.println("[AuctionServer] Lỗi TIME_SYNC: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);

        System.out.println("[AuctionServer] ⏱ TIME_SYNC broadcaster đã khởi động (mỗi 1 giây).");
    }

    // =====================================================
    // BROADCAST
    // =====================================================

    /**
     * Gửi message tới TẤT CẢ client đang kết nối.
     * Dùng cho các sự kiện cần đồng bộ: BID_UPDATE, AUCTION_CLOSED, ...
     */
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Gửi message tới tất cả client ĐANG THEO DÕI một auctionId cụ thể.
     */
    public void broadcastToAuction(int auctionId, String message) {
        for (ClientHandler client : clients) {
            if (client.getWatchingAuctionId() == auctionId) {
                client.sendMessage(message);
            }
        }
    }

    // =====================================================
    // QUẢN LÝ CLIENT
    // =====================================================

    /**
     * Xóa client khỏi danh sách khi disconnect.
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[AuctionServer] 📤 Client đã ngắt kết nối. "
                + "Còn lại: " + clients.size() + " client(s).");
    }

    public boolean isRunning() {
        return running;
    }

    public int getClientCount() {
        return clients.size();
    }

    // =====================================================
    // MAIN (chạy server độc lập nếu cần test)
    // =====================================================

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer(9999);
        server.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("Nhấn Ctrl+C để dừng server.");
    }
}
