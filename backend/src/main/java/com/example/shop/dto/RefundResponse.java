package com.example.shop.dto;

import java.math.BigDecimal;

public record RefundResponse(
    Long orderId, BigDecimal amount, String currency, String refundId, String status) {}
