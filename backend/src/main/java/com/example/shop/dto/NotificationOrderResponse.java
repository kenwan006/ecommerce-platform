package com.example.shop.dto;

import java.math.BigDecimal;

/** Minimal customer/order data exposed to the internal Notification service. */
public record NotificationOrderResponse(
    Long orderId, String customerName, String customerEmail, BigDecimal total, String currency) {}
