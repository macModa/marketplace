CREATE TABLE IF NOT EXISTS delivery_qr_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(32) NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    version BIGINT
);
