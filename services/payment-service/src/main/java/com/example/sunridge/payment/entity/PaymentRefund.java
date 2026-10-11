package com.example.sunridge.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** A separate money-movement record; it never replaces the original payment. */
@Entity
@Table(name = "payment_refunds")
public class PaymentRefund {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @Column(name = "commerce_order_id")
  private Long commerceOrderId;

  @Column(name = "commerce_return_id")
  private Long commerceReturnId;

  @Column(name = "provider_refund_id")
  private String providerRefundId;

  private BigDecimal amount;
  private String currency;
  private String status;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  public Payment getPayment() {
    return payment;
  }

  public String getProviderRefundId() {
    return providerRefundId;
  }

  public Long getCommerceOrderId() {
    return commerceOrderId;
  }

  public Long getCommerceReturnId() {
    return commerceReturnId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getStatus() {
    return status;
  }

  public void setPayment(Payment payment) {
    this.payment = payment;
  }

  public void setCommerceOrderId(Long commerceOrderId) {
    this.commerceOrderId = commerceOrderId;
  }

  public void setCommerceReturnId(Long commerceReturnId) {
    this.commerceReturnId = commerceReturnId;
  }

  public void setProviderRefundId(String providerRefundId) {
    this.providerRefundId = providerRefundId;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }
}
