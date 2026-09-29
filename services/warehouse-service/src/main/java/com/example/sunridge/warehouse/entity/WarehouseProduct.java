package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_products")
public class WarehouseProduct {
  @Id private Long productId;
  private String sku;
  private String name;
  private boolean active = true;

  public Long getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
