CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    original_price DECIMAL(10, 2) NOT NULL,
    owner_id INT,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10, 2) NOT NULL,
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id INT,
    item_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);
