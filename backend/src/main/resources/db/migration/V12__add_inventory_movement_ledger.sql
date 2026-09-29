CREATE TABLE inventory_movements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  note TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_inventory_movement_product FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT INTO inventory_movements (warehouse_id, product_id, type, quantity, note)
SELECT warehouse_id, product_id, 'INITIAL_LOAD', on_hand_quantity, 'Initial stock migrated from products.stock'
FROM inventory_levels;
