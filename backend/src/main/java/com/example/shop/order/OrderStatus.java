package com.example.shop.order;

/** Customer-facing lifecycle owned by the Commerce service. */
public enum OrderStatus {
  PENDING_PAYMENT,
  FRAUD_REVIEW,
  PAID,
  PAYMENT_FAILED,
  FRAUD_DECLINED,
  CANCELLED
}
