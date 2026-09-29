CREATE TABLE fulfillment_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fulfillment_order_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_fulfillment_order_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE fulfillment_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fulfillment_order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  CONSTRAINT fk_fulfillment_item_order FOREIGN KEY (fulfillment_order_id) REFERENCES fulfillment_orders(id),
  CONSTRAINT fk_fulfillment_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE shipments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fulfillment_order_id BIGINT NOT NULL UNIQUE,
  carrier VARCHAR(100) NOT NULL,
  tracking_number VARCHAR(255) NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  shipped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_shipment_fulfillment FOREIGN KEY (fulfillment_order_id) REFERENCES fulfillment_orders(id)
);
