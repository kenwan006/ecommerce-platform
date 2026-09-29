package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_reservation_items")
public class InventoryReservationItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "reservation_id")
  private InventoryReservation reservation;

  private Long productId;
  private int quantity;

  public Long getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setReservation(InventoryReservation reservation) {
    this.reservation = reservation;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
