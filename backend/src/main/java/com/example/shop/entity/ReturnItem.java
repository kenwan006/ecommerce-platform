package com.example.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "return_items")
public class ReturnItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "return_id")
  private Return returnRequest;

  private Long productId;
  private String productName;
  private int quantity;

  public Long getProductId() { return productId; }
  public String getProductName() { return productName; }
  public int getQuantity() { return quantity; }
  public void setReturnRequest(Return returnRequest) { this.returnRequest = returnRequest; }
  public void setProductId(Long productId) { this.productId = productId; }
  public void setProductName(String productName) { this.productName = productName; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
}
