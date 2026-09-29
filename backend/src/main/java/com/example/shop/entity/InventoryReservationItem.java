package com.example.shop.entity;

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

  @ManyToOne(optional = false)
  @JoinColumn(name = "reservation_id")
  private InventoryReservation reservation;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  private int quantity;

  public Product getProduct() { return product; }
  public int getQuantity() { return quantity; }
  public void setReservation(InventoryReservation reservation) { this.reservation = reservation; }
  public void setProduct(Product product) { this.product = product; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
}
