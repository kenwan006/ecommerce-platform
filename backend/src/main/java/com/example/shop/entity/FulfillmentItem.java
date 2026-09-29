package com.example.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfillment_items")
public class FulfillmentItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fulfillment_order_id")
  private FulfillmentOrder fulfillmentOrder;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  private int quantity;
  private String status = "PENDING_PICK";

  public Product getProduct() { return product; }
  public int getQuantity() { return quantity; }
  public void setFulfillmentOrder(FulfillmentOrder fulfillmentOrder) { this.fulfillmentOrder = fulfillmentOrder; }
  public void setProduct(Product product) { this.product = product; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public void setStatus(String status) { this.status = status; }
}
