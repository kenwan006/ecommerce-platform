package com.example.shop.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
    Long orderId,
    String paymentIntentId,
    String clientSecret,
    BigDecimal total,
    String currency,
    String status) {}
