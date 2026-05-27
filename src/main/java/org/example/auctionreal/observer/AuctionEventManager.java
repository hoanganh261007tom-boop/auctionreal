package org.example.auctionreal.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AuctionEventManager – Observable (Subject) trong Observer Pattern.
 *
 * Quản lý danh sách observers và thông báo khi có sự kiện đấu giá.
 * Kết hợp Singleton + Observer để đảm bảo chỉ có 1 event manager toàn app.
 *
 * Áp dụng Design Pattern: Observer + Singleton
 *
 * Cách dùng:
 *   // Đăng ký observer
 *   AuctionEventManager.getInstance().addObserver(myController);
 *
 *   // Thông báo bid mới
 *   AuctionEventManager.getInstance().notifyBidUpdated(auctionId, price, bidder);
 */
public class AuctionEventManager {

    // ── Singleton ──
    private static volatile AuctionEventManager instance = null;

    /** Danh sách observers – CopyOnWriteArrayList đảm bảo thread-safe */
    private final List<BidObserver> observers = new CopyOnWriteArrayList<>();

    private AuctionEventManager() {}

    public static AuctionEventManager getInstance() {
        if (instance == null) {
            synchronized (AuctionEventManager.class) {
                if (instance == null) {
                    instance = new AuctionEventManager();
                }
            }
        }
        return instance;
    }

    // ── Quản lý observers ──

    public void addObserver(BidObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("[EventManager] ➕ Observer đăng ký: "
                    + observer.getClass().getSimpleName());
        }
    }

    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
        System.out.println("[EventManager] ➖ Observer hủy đăng ký: "
                + observer.getClass().getSimpleName());
    }

    // ── Thông báo sự kiện (notify) ──

    /**
     * Thông báo bid mới tới tất cả observers đang đăng ký.
     */
    public void notifyBidUpdated(int auctionId, double newPrice, String bidderName) {
        System.out.println("[EventManager] 📢 Notify BID_UPDATE: phiên #"
                + auctionId + " → " + newPrice + "₫ bởi " + bidderName);
        for (BidObserver obs : observers) {
            obs.onBidUpdated(auctionId, newPrice, bidderName);
        }
    }

    /**
     * Thông báo phiên đấu giá đã đóng.
     */
    public void notifyAuctionClosed(int auctionId, String winner) {
        System.out.println("[EventManager] 📢 Notify AUCTION_CLOSED: phiên #"
                + auctionId + " → Người thắng: " + winner);
        for (BidObserver obs : observers) {
            obs.onAuctionClosed(auctionId, winner);
        }
    }

    public int getObserverCount() { return observers.size(); }
}
