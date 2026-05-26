-- ============================================================
-- init.sql - Khởi tạo database cho dự án AuctionReal
-- Chạy file này trên MySQL Workbench để tạo toàn bộ database
-- ============================================================

CREATE DATABASE IF NOT EXISTS auctiondb_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auctiondb_local;

-- ============================================================
-- BẢNG users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
                                     user_id  INT          NOT NULL AUTO_INCREMENT,
                                     username VARCHAR(50)  NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     ENUM('ADMIN','SELLER','BIDDER') NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- BẢNG items
-- ============================================================
CREATE TABLE IF NOT EXISTS items (
                                     item_id       INT            NOT NULL AUTO_INCREMENT,
                                     name          VARCHAR(255)   NOT NULL,
    starting_price DOUBLE        NOT NULL,
    status        ENUM('OPEN','CLOSED') DEFAULT 'OPEN',
    seller_id     INT            DEFAULT NULL,
    description   TEXT,
    current_price DOUBLE         DEFAULT 0,
    min_step      DOUBLE         DEFAULT 1000000,
    PRIMARY KEY (item_id),
    KEY fk_seller (seller_id),
    CONSTRAINT fk_seller FOREIGN KEY (seller_id) REFERENCES users (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- BẢNG auctions
-- ============================================================
CREATE TABLE IF NOT EXISTS auctions (
                                        auction_id  INT          NOT NULL AUTO_INCREMENT,
                                        item_id     INT          DEFAULT NULL,
                                        start_time  DATETIME     NOT NULL,
                                        end_time    DATETIME     NOT NULL,
                                        current_bid DOUBLE       DEFAULT 0,
                                        status      VARCHAR(20)  DEFAULT 'OPEN',
    duration    INT          DEFAULT 2,
    PRIMARY KEY (auction_id),
    KEY fk_auction_item (item_id),
    CONSTRAINT auctions_ibfk_1 FOREIGN KEY (item_id) REFERENCES items (item_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- BẢNG bids
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
                                    bid_id     INT       NOT NULL AUTO_INCREMENT,
                                    auction_id INT       NOT NULL,
                                    bidder_id  INT       NOT NULL,
                                    bid_amount DOUBLE    NOT NULL,
                                    bid_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (bid_id),
    KEY fk_bid_auction (auction_id),
    KEY fk_bid_bidder  (bidder_id),
    CONSTRAINT bids_ibfk_1 FOREIGN KEY (auction_id) REFERENCES auctions (auction_id),
    CONSTRAINT bids_ibfk_2 FOREIGN KEY (bidder_id)  REFERENCES users    (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- BẢNG watchlist
-- ============================================================
CREATE TABLE IF NOT EXISTS watchlist (
                                         watch_id   INT NOT NULL AUTO_INCREMENT,
                                         user_id    INT NOT NULL,
                                         auction_id INT NOT NULL,
                                         PRIMARY KEY (watch_id),
    KEY fk_watch_user    (user_id),
    KEY fk_watch_auction (auction_id),
    CONSTRAINT watchlist_ibfk_1 FOREIGN KEY (user_id)    REFERENCES users    (user_id),
    CONSTRAINT watchlist_ibfk_2 FOREIGN KEY (auction_id) REFERENCES auctions (auction_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- DỮ LIỆU MẪU (tùy chọn - xóa nếu không cần)
-- ============================================================

-- Tài khoản mẫu (password chưa mã hóa, chỉ dùng để test)
INSERT IGNORE INTO users (username, password, role) VALUES
    ('admin',   'admin123', 'ADMIN'),
    ('seller1', '123456',   'SELLER'),
    ('bidder1', '123456',   'BIDDER');