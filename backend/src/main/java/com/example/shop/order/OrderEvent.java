package com.example.shop.order;

/** Business facts that may cause an order-state transition. */
public enum OrderEvent {
  PAYMENT_SUCCEEDED,
  PAYMENT_FAILED,
  FRAUD_REVIEW_REQUIRED,
  FRAUD_DECLINED,
  REVIEW_APPROVED,
  REVIEW_DECLINED,
  CANCELLED
}
