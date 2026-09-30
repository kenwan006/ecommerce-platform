package com.example.shop.dto;

import java.math.BigDecimal;
import com.example.shop.order.OrderStatus;

public record CheckoutResponse(
    Long orderId,
    String paymentIntentId,
    String clientSecret,
    BigDecimal total,
    String currency,
    OrderStatus status) {}
