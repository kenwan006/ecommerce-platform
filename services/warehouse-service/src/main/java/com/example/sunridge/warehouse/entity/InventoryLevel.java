package com.example.sunridge.warehouse.entity;

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

  @ManyToOne
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private WarehouseProduct product;

  private int onHandQuantity;
  private int reservedQuantity;
  @Version private long version;

  public WarehouseProduct getProduct() {
    return product;
  }

  public int getOnHandQuantity() {
    return onHandQuantity;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }

  public int availableQuantity() {
    return onHandQuantity - reservedQuantity;
  }

  public void setWarehouse(Warehouse warehouse) {
    this.warehouse = warehouse;
  }

  public void setProduct(WarehouseProduct product) {
    this.product = product;
  }

  public void receive(int quantity) {
    onHandQuantity += quantity;
  }

  public void reserve(int quantity) {
    if (availableQuantity() < quantity)
      throw new IllegalStateException("Insufficient inventory for " + product.getName());
    reservedQuantity += quantity;
  }

  public void release(int quantity) {
    reservedQuantity -= quantity;
    if (reservedQuantity < 0) throw new IllegalStateException("Invalid reservation state");
  }

  public void ship(int quantity) {
    release(quantity);
    onHandQuantity -= quantity;
    if (onHandQuantity < 0) throw new IllegalStateException("Invalid inventory state");
  }

  public void export(int quantity) {
    if (availableQuantity() < quantity)
      throw new IllegalStateException("Cannot export reserved inventory");
    onHandQuantity -= quantity;
  }
}
