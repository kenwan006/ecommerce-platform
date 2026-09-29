package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long warehouseId;
  private Long productId;
  private String type;
  private int quantity;
  private String note;
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public Long getProductId() {
    return productId;
  }

  public String getType() {
    return type;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getNote() {
    return note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setWarehouseId(Long warehouseId) {
    this.warehouseId = warehouseId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
