-- Warehouse owns operational inventory and fulfillment data in sunridge_warehouse.
-- These were interim monolith tables in the Commerce database.
DROP TABLE IF EXISTS shipments;
DROP TABLE IF EXISTS fulfillment_items;
DROP TABLE IF EXISTS fulfillment_orders;
DROP TABLE IF EXISTS inventory_movements;
DROP TABLE IF EXISTS inventory_reservation_items;
DROP TABLE IF EXISTS inventory_reservations;
DROP TABLE IF EXISTS inventory_levels;
DROP TABLE IF EXISTS warehouses;
