package com.example.sunridge.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long commerceOrderId;
  private String checkoutId;
  private String provider;
  private String providerPaymentId;
  private BigDecimal amount;
  private String currency;
  private String status;
  private String providerRefundId;

  public Long getId() {
    return id;
  }

  public Long getCommerceOrderId() {
    return commerceOrderId;
  }

  public String getCheckoutId() {
    return checkoutId;
  }

  public String getProviderPaymentId() {
    return providerPaymentId;
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
  public String getProviderRefundId() { return providerRefundId; }

  public void setCommerceOrderId(Long commerceOrderId) {
    this.commerceOrderId = commerceOrderId;
  }

  public void setCheckoutId(String checkoutId) {
    this.checkoutId = checkoutId;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public void setProviderPaymentId(String providerPaymentId) {
    this.providerPaymentId = providerPaymentId;
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
  public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }
}
