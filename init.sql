-- ============================================================
--  AuctionReal – Database Schema
--  Chạy file này trong MySQL Workbench hoặc HeidiSQL
--  Database: auctiondb_local  (khớp với DatabaseConnection.java)
-- ============================================================

CREATE DATABASE IF NOT EXISTS auctiondb_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auctiondb_local;

-- ─── Bảng người dùng ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id         INT          AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- lưu hash hoặc plaintext (dev)
    role       VARCHAR(20)  NOT NULL DEFAULT 'BIDDER',  -- 'BIDDER' | 'SELLER'
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- ─── Bảng vật phẩm đấu giá ──────────────────────────────────
CREATE TABLE IF NOT EXISTS items (
    id             INT           AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255)  NOT NULL,
    description    TEXT,
    original_price DECIMAL(15,2) NOT NULL,
    category       VARCHAR(100),
    item_condition VARCHAR(50)   DEFAULT 'Như mới',
    duration_mins  INT           DEFAULT 60,
    status         VARCHAR(50)   DEFAULT 'Đang đấu giá',
    owner_id       INT,
    created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ─── Bảng lịch sử đặt giá ───────────────────────────────────
CREATE TABLE IF NOT EXISTS bids (
    id       INT           AUTO_INCREMENT PRIMARY KEY,
    amount   DECIMAL(15,2) NOT NULL,
    bid_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    user_id  INT,
    item_id  INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- ─── Dữ liệu mẫu (tuỳ chọn – xoá nếu không cần) ────────────
INSERT IGNORE INTO users (username, password, role) VALUES
    ('admin',   'admin123',  'SELLER'),
    ('bidder1', 'pass1234',  'BIDDER'),
    ('seller1', 'pass1234',  'SELLER');

INSERT IGNORE INTO items (name, description, original_price, category, item_condition, duration_mins, owner_id)
VALUES
    ('Rolex Submariner 2023', 'Đồng hồ lặn biển, bezel ceramic, mới 98%.', 285000000, 'Đồng hồ', 'Như mới', 120, 1),
    ('iPhone 15 Pro Max 256GB', 'Máy nguyên seal titan tự nhiên, BH 12 tháng.', 35000000, 'Điện tử', 'Mới 100%', 60, 1),
    ('Nhẫn kim cương 2 carat GIA', 'Kim cương thiên nhiên, vàng trắng 18K.', 450000000, 'Trang sức', 'Mới 100%', 180, 3);
