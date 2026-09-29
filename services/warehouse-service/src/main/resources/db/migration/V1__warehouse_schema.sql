CREATE TABLE warehouse_products (
  product_id BIGINT PRIMARY KEY,
  sku VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE warehouses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL
);

INSERT INTO warehouses (code, name) VALUES ('MAIN', 'Main Warehouse');

CREATE TABLE inventory_levels (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  on_hand_quantity INT NOT NULL,
  reserved_quantity INT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_warehouse_inventory UNIQUE (warehouse_id, product_id),
  CONSTRAINT fk_warehouse_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_warehouse_inventory_product FOREIGN KEY (product_id) REFERENCES warehouse_products(product_id)
);

CREATE TABLE inventory_reservations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  commerce_order_id BIGINT NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_reservation_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_warehouse_reservation_item FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id),
  CONSTRAINT fk_warehouse_reservation_product FOREIGN KEY (product_id) REFERENCES warehouse_products(product_id)
);

CREATE TABLE inventory_movements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  reference_type VARCHAR(50) NULL,
  reference_id VARCHAR(100) NULL,
  note TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_warehouse_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_warehouse_movement_product FOREIGN KEY (product_id) REFERENCES warehouse_products(product_id)
);

CREATE TABLE fulfillment_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  commerce_order_id BIGINT NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fulfillment_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fulfillment_order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  CONSTRAINT fk_warehouse_fulfillment_item FOREIGN KEY (fulfillment_order_id) REFERENCES fulfillment_orders(id)
);

CREATE TABLE shipments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fulfillment_order_id BIGINT NOT NULL UNIQUE,
  carrier VARCHAR(100) NOT NULL,
  tracking_number VARCHAR(255) NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  shipped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_warehouse_shipment_fulfillment FOREIGN KEY (fulfillment_order_id) REFERENCES fulfillment_orders(id)
);
