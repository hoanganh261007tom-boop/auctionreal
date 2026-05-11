    id         INT          AUTO_INCREMENT PRIMARY KEY,
);

    id             INT           AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255)  NOT NULL,
    owner_id       INT,
);

    id       INT           AUTO_INCREMENT PRIMARY KEY,
    bid_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    user_id  INT,
    item_id  INT,
);
