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
  CONSTRAINT uk_inventory_level UNIQUE (warehouse_id, product_id),
  CONSTRAINT fk_inventory_level_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_inventory_level_product FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT INTO inventory_levels (warehouse_id, product_id, on_hand_quantity, reserved_quantity, version)
SELECT (SELECT id FROM warehouses WHERE code = 'MAIN'), id, stock, 0, 0
FROM products;

CREATE TABLE inventory_reservations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_inventory_reservation_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_inventory_reservation_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE inventory_reservation_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_reservation_item_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id),
  CONSTRAINT fk_reservation_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);
