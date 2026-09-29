package com.example.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_levels")
public class InventoryLevel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  private int onHandQuantity;
  private int reservedQuantity;

  @Version
  private long version;

  public Product getProduct() { return product; }
  public int getOnHandQuantity() { return onHandQuantity; }
  public int getReservedQuantity() { return reservedQuantity; }
  public int getAvailableQuantity() { return onHandQuantity - reservedQuantity; }

  public void reserve(int quantity) {
    if (getAvailableQuantity() < quantity) {
      throw new IllegalStateException(product.getName() + " is out of stock");
    }
    reservedQuantity += quantity;
  }

  public void release(int quantity) {
    reservedQuantity -= quantity;
    if (reservedQuantity < 0) {
      throw new IllegalStateException("Inventory reservation is inconsistent");
    }
  }

  public void ship(int quantity) {
    release(quantity);
    removeOnHand(quantity);
  }

  public void receive(int quantity) {
    onHandQuantity += quantity;
    product.setStock(onHandQuantity);
  }

  public void export(int quantity) {
    if (getAvailableQuantity() < quantity) {
      throw new IllegalStateException(
          "Cannot export " + quantity + " units of " + product.getName() + "; some units are reserved");
    }
    removeOnHand(quantity);
  }

  private void removeOnHand(int quantity) {
    onHandQuantity -= quantity;
    if (onHandQuantity < 0) {
      throw new IllegalStateException("Inventory on-hand quantity is inconsistent");
    }
    product.setStock(onHandQuantity);
  }
}
