package com.example.sunridge.warehouse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long commerceOrderId;
  private String status;
  private Instant expiresAt;

  @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InventoryReservationItem> items = new ArrayList<>();

  public Long getCommerceOrderId() {
    return commerceOrderId;
  }

  public String getStatus() {
    return status;
  }

  public List<InventoryReservationItem> getItems() {
    return items;
  }

  public void setCommerceOrderId(Long commerceOrderId) {
    this.commerceOrderId = commerceOrderId;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void addItem(InventoryReservationItem item) {
    items.add(item);
    item.setReservation(this);
  }
}
