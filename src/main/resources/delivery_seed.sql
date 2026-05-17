-- =============================================================================
-- Delivery Module — Seed Data (MySQL)
-- =============================================================================
-- Run AFTER delivery_schema.sql (or after Hibernate has created the tables).
-- Covers hubs, relay points, postal zones for Tunis, Bizerte, Nabeul.
-- =============================================================================

-- ─── Hubs ─────────────────────────────────────────────────────────────────────

INSERT INTO delivery_hubs (name, governorate, address, latitude, longitude, active) VALUES
('Tunis Hub Central',  'GRAND_TUNIS', 'Zone Industrielle La Charguia 2, Tunis 2035', 36.8620, 10.1980, true),
('Bizerte Hub',        'BIZERTE',     'Route de Tunis, Zone Industrielle Bizerte 7000', 37.2741, 9.8739, true),
('Nabeul Hub',         'NABEUL',      'Boulevard Taïeb Mehiri, Nabeul 8000', 36.4561, 10.7356, true);

-- ─── Relay Points for Grand Tunis (postal: 1002, 1004) ───────────────────────

-- Postal 1002 — Tunis Belvedère
INSERT INTO delivery_relay_points (name, address, postal_code, latitude, longitude, max_capacity, current_load, active, hub_id)
VALUES
('Relais Belvédère',  '12 Avenue du Belvédère, Tunis 1002',   '1002', 36.8176, 10.1660, 80, 12, true, 1),
('Relais El Menzah',  '5 Rue d''Algérie, El Menzah 1002',     '1002', 36.8302, 10.1850, 60, 8,  true, 1);

-- Postal 1004 — El Menzah / La Marsa
INSERT INTO delivery_relay_points (name, address, postal_code, latitude, longitude, max_capacity, current_load, active, hub_id)
VALUES
('Relais La Marsa',   '23 Rue de la Liberté, La Marsa 1004',  '1004', 36.8879, 10.3228, 100, 45, true, 1),
('Relais Carthage',   '7 Avenue Habib Bourguiba, Carthage 1004','1004', 36.8588, 10.2944, 50,  5, true, 1);

-- ─── Relay Points for Bizerte (postal: 7000, 7010) ───────────────────────────

-- Postal 7000 — Bizerte Ville
INSERT INTO delivery_relay_points (name, address, postal_code, latitude, longitude, max_capacity, current_load, active, hub_id)
VALUES
('Relais Bizerte Centre', '45 Avenue Taieb Mhiri, Bizerte 7000', '7000', 37.2747, 9.8739, 80,  20, true, 2),
('Relais Zarzouna',       '8 Rue de la Gare, Zarzouna 7000',     '7000', 37.2610, 9.8541, 40,  3,  true, 2);

-- Postal 7010 — Menzel Bourguiba (Bizerte subzone)
INSERT INTO delivery_relay_points (name, address, postal_code, latitude, longitude, max_capacity, current_load, active, hub_id)
VALUES
('Relais Menzel Bourguiba', '12 Rue de la République, Menzel Bourguiba 7010', '7010', 37.1620, 9.7880, 60, 15, true, 2);

-- ─── Relay Points for Nabeul (postal: 8000) ──────────────────────────────────

INSERT INTO delivery_relay_points (name, address, postal_code, latitude, longitude, max_capacity, current_load, active, hub_id)
VALUES
('Relais Nabeul Centre',  '3 Avenue Habib Bourguiba, Nabeul 8000', '8000', 36.4561, 10.7356, 100, 30, true, 3),
('Relais Nabeul Hammamet','15 Route de Hammamet, Nabeul 8000',     '8000', 36.4001, 10.6167, 70,  10, true, 3);

-- ─── Postal Zones ─────────────────────────────────────────────────────────────

INSERT INTO delivery_postal_zones (postal_code, governorate, zone_name) VALUES
-- Grand Tunis
('1000', 'GRAND_TUNIS', 'Tunis Centre'),
('1001', 'GRAND_TUNIS', 'Tunis Médina'),
('1002', 'GRAND_TUNIS', 'Tunis Belvédère'),
('1003', 'GRAND_TUNIS', 'Tunis Lafayette'),
('1004', 'GRAND_TUNIS', 'La Marsa / El Menzah'),
('1005', 'GRAND_TUNIS', 'Tunis El Omrane'),
('1006', 'GRAND_TUNIS', 'Le Bardo'),
('1080', 'GRAND_TUNIS', 'Tunis Nord'),
-- Bizerte
('7000', 'BIZERTE', 'Bizerte Ville'),
('7010', 'BIZERTE', 'Menzel Bourguiba'),
('7020', 'BIZERTE', 'Mateur'),
('7030', 'BIZERTE', 'Ras Jebel'),
-- Nabeul
('8000', 'NABEUL', 'Nabeul Ville'),
('8011', 'NABEUL', 'Hammamet'),
('8020', 'NABEUL', 'Kélibia'),
('8030', 'NABEUL', 'Dar Chaâbane');
