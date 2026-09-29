package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

  @OneToOne(optional = false)
  @JoinColumn(name = "order_id", unique = true)
  private Order order;

  @ManyToOne(optional = false)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @Column(nullable = false)
  private String status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InventoryReservationItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public Order getOrder() { return order; }
  public String getStatus() { return status; }
  public Instant getExpiresAt() { return expiresAt; }
  public List<InventoryReservationItem> getItems() { return items; }

  public void setOrder(Order order) { this.order = order; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public void setStatus(String status) { this.status = status; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public void addItem(InventoryReservationItem item) { items.add(item); item.setReservation(this); }
}
