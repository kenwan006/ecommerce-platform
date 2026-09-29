package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private int quantity;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Product getProduct() { return product; }
  public String getType() { return type; }
  public int getQuantity() { return quantity; }
  public String getNote() { return note; }
  public Instant getCreatedAt() { return createdAt; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public void setProduct(Product product) { this.product = product; }
  public void setType(String type) { this.type = type; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public void setNote(String note) { this.note = note; }
}
