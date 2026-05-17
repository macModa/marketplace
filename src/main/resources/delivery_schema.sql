-- =============================================================================
-- Delivery Module — MySQL Schema
-- =============================================================================
-- Run this ONCE against marketplace_db before first application startup,
-- OR let Hibernate create tables via ddl-auto: update and only run seed data.
--
-- Table partitioning note:
--   delivery_parcels is designed with postal_code so it can be range-partitioned
--   by postal prefix in a future dedicated MySQL server once volume justifies it.
--   Example partition key: LEFT(postal_code, 2)
-- =============================================================================

-- ─── Hubs ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS delivery_hubs (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    governorate VARCHAR(30)     NOT NULL,
    address     VARCHAR(255)    NOT NULL,
    latitude    DOUBLE          NOT NULL,
    longitude   DOUBLE          NOT NULL,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    INDEX idx_hub_governorate (governorate),
    INDEX idx_hub_active       (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Relay Points ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS delivery_relay_points (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)    NOT NULL,
    address       VARCHAR(255)    NOT NULL,
    postal_code   VARCHAR(10)     NOT NULL,
    latitude      DOUBLE          NOT NULL,
    longitude     DOUBLE          NOT NULL,
    max_capacity  INT             NOT NULL DEFAULT 100,
    current_load  INT             NOT NULL DEFAULT 0,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    hub_id        BIGINT          NOT NULL,
    PRIMARY KEY (id),
    INDEX  idx_relay_postal_code (postal_code),
    INDEX  idx_relay_hub         (hub_id),
    INDEX  idx_relay_active      (active),
    CONSTRAINT fk_relay_hub FOREIGN KEY (hub_id) REFERENCES delivery_hubs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Postal Zones ─────────────────────────────────────────────────────────────
-- Partitionable by LEFT(postal_code, 2) for future horizontal scaling

CREATE TABLE IF NOT EXISTS delivery_postal_zones (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    postal_code VARCHAR(10)     NOT NULL,
    governorate VARCHAR(30)     NOT NULL,
    zone_name   VARCHAR(100)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_postal_zone_code        (postal_code),
    INDEX        idx_postal_zone_governorate  (governorate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Parcels ──────────────────────────────────────────────────────────────────
-- Future partition candidate: PARTITION BY RANGE (CAST(LEFT(postal_code,2) AS UNSIGNED))
-- Requires MySQL 8+ and carefully managed partition definitions.

CREATE TABLE IF NOT EXISTS delivery_parcels (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    tracking_number  VARCHAR(30)     NOT NULL,
    order_id         BIGINT          NOT NULL,
    recipient_name   VARCHAR(150)    NOT NULL,
    recipient_phone  VARCHAR(20)     NOT NULL,
    delivery_address VARCHAR(300)    NOT NULL,
    postal_code      VARCHAR(10)     NOT NULL,
    weight_kg        DECIMAL(6,2)    NOT NULL DEFAULT 1.00,
    status           VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    estimated_delivery DATE          NULL,
    hub_id           BIGINT          NULL,
    relay_point_id   BIGINT          NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_parcel_tracking  (tracking_number),
    INDEX        idx_parcel_status    (status),
    INDEX        idx_parcel_postal    (postal_code),
    INDEX        idx_parcel_order     (order_id),
    CONSTRAINT fk_parcel_hub   FOREIGN KEY (hub_id)          REFERENCES delivery_hubs(id),
    CONSTRAINT fk_parcel_relay FOREIGN KEY (relay_point_id)  REFERENCES delivery_relay_points(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Tracking Events ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS delivery_tracking_events (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    parcel_id   BIGINT          NOT NULL,
    status      VARCHAR(30)     NOT NULL,
    description VARCHAR(500)    NOT NULL,
    location    VARCHAR(200)    NULL,
    occurred_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_tracking_parcel      (parcel_id),
    INDEX idx_tracking_status      (status),
    INDEX idx_tracking_occurred_at (occurred_at),
    CONSTRAINT fk_tracking_parcel FOREIGN KEY (parcel_id) REFERENCES delivery_parcels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
