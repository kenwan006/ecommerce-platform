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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "orders",
    uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_orders_user_checkout_id",
        columnNames = {"user_id", "checkout_id"}))
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  private BigDecimal total;
  private String status = "PENDING";
  @Column(name = "stripe_payment_intent_id")
  private String stripePaymentIntentId;
  @Column(name = "checkout_id", nullable = false)
  private String checkoutId;
  @Column(name = "payment_status")
  private String paymentStatus = "PENDING";
  @Column(name = "stock_reserved")
  private boolean stockReserved;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public User getUser() { return user; }
  public BigDecimal getTotal() { return total; }
  public String getStatus() { return status; }
  public String getStripePaymentIntentId() { return stripePaymentIntentId; }
  public String getCheckoutId() { return checkoutId; }
  public String getPaymentStatus() { return paymentStatus; }
  public boolean isStockReserved() { return stockReserved; }
  public Instant getCreatedAt() { return createdAt; }
  public List<OrderItem> getItems() { return items; }

  public void setUser(User user) { this.user = user; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public void setStatus(String status) { this.status = status; }
  public void setStripePaymentIntentId(String stripePaymentIntentId) {
    this.stripePaymentIntentId = stripePaymentIntentId;
  }
  public void setCheckoutId(String checkoutId) { this.checkoutId = checkoutId; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
  public void setStockReserved(boolean stockReserved) { this.stockReserved = stockReserved; }
  public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
}
