package org.example.auctionreal.network;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * SocketClient: Client socket dùng trong JavaFX controller.
 * Kết nối tới AuctionServer, gửi lệnh và nhận push updates.
 *
 * Tất cả callback được gọi trên JavaFX Application Thread
 * thông qua Platform.runLater().
 */
public class SocketClient {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread listenerThread;
    private volatile boolean connected = false;

    /** Callback khi nhận message từ server. */
    private Consumer<String> onMessageReceived;

    /** Callback khi kết nối bị ngắt. */
    private Runnable onDisconnected;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // =====================================================
    // KẾT NỐI / NGẮT KẾT NỐI
    // =====================================================

    /**
     * Kết nối tới server. Chạy trên thread gọi.
     * @return true nếu kết nối thành công
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            connected = true;

            System.out.println("[SocketClient] ✅ Đã kết nối tới server " + host + ":" + port);

            // Thread lắng nghe message từ server
            listenerThread = new Thread(this::listenForMessages, "SocketClient-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            return true;

        } catch (IOException e) {
            System.err.println("[SocketClient] ❌ Không thể kết nối tới " + host + ":" + port
                    + " → " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * Ngắt kết nối sạch sẽ.
     */
    public void disconnect() {
        if (!connected) return;
        connected = false;

        // Gửi lệnh LEAVE trước khi ngắt
        sendMessage("LEAVE");

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }

        System.out.println("[SocketClient] 🔌 Đã ngắt kết nối.");
    }

    // =====================================================
    // GỬI MESSAGE
    // =====================================================

    /**
     * Gửi message tới server.
     */
    public void sendMessage(String message) {
        if (writer != null && connected) {
            writer.println(message);
            writer.flush();
            System.out.println("[SocketClient] 📤 Gửi: " + message);
        }
    }

    /**
     * Gửi lệnh JOIN khi vào phiên đấu giá.
     */
    public void joinAuction(int auctionId, int userId, String username) {
        sendMessage("JOIN:" + auctionId + ":" + userId + ":" + username);
    }

    /**
     * Gửi lệnh BID.
     */
    public void placeBid(int auctionId, int bidderId, double amount) {
        sendMessage("BID:" + auctionId + ":" + bidderId + ":" + amount);
    }

    // =====================================================
    // LẮNG NGHE MESSAGE TỪ SERVER
    // =====================================================

    private void listenForMessages() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                final String message = line.trim();
                if (message.isEmpty()) continue;

                System.out.println("[SocketClient] 📩 Nhận: " + message);

                // Gọi callback trên JavaFX thread
                if (onMessageReceived != null) {
                    Platform.runLater(() -> onMessageReceived.accept(message));
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("[SocketClient] 🔌 Mất kết nối: " + e.getMessage());
            }
        } finally {
            connected = false;
            if (onDisconnected != null) {
                Platform.runLater(onDisconnected);
            }
        }
    }

    // =====================================================
    // CALLBACKS
    // =====================================================

    /**
     * Đặt callback xử lý message nhận được từ server.
     * Callback sẽ được gọi trên JavaFX Application Thread.
     */
    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }

    /**
     * Đặt callback khi kết nối bị ngắt.
     * Callback sẽ được gọi trên JavaFX Application Thread.
     */
    public void setOnDisconnected(Runnable callback) {
        this.onDisconnected = callback;
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public boolean isConnected() {
        return connected;
    }
}
