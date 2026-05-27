package org.example.auctionreal.observer;

/**
 * BidObserver – Observer Pattern (interface).
 *
 * Bất kỳ class nào muốn nhận thông báo khi có bid mới
 * đều implements interface này.
 *
 * Áp dụng Design Pattern: Observer
 */
public interface BidObserver {

    /**
     * Được gọi khi có bid mới trong phiên đấu giá.
     * @param auctionId  ID phiên đấu giá
     * @param newPrice   Giá mới vừa được đặt
     * @param bidderName Tên người đặt giá
     */
    void onBidUpdated(int auctionId, double newPrice, String bidderName);

    /**
     * Được gọi khi phiên đấu giá kết thúc.
     * @param auctionId ID phiên đấu giá
     * @param winner    Tên người thắng
     */
    void onAuctionClosed(int auctionId, String winner);
}
