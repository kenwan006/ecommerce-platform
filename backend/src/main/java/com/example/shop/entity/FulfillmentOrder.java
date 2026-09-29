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
@Table(name = "fulfillment_orders")
public class FulfillmentOrder {
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

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "fulfillmentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FulfillmentItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public Order getOrder() { return order; }
  public String getStatus() { return status; }
  public List<FulfillmentItem> getItems() { return items; }
  public void setOrder(Order order) { this.order = order; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public void setStatus(String status) { this.status = status; }
  public void addItem(FulfillmentItem item) { items.add(item); item.setFulfillmentOrder(this); }
}
