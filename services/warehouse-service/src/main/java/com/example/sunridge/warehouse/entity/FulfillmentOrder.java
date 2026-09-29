package com.example.sunridge.warehouse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fulfillment_orders")
public class FulfillmentOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long commerceOrderId;
  private String status;

  @OneToMany(mappedBy = "fulfillmentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FulfillmentItem> items = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public Long getCommerceOrderId() {
    return commerceOrderId;
  }

  public String getStatus() {
    return status;
  }

  public List<FulfillmentItem> getItems() {
    return items;
  }

  public void setCommerceOrderId(Long commerceOrderId) {
    this.commerceOrderId = commerceOrderId;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void addItem(FulfillmentItem item) {
    items.add(item);
    item.setFulfillmentOrder(this);
  }
}
