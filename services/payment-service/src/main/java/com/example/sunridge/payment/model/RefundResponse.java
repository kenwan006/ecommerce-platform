package com.example.sunridge.payment.model;

import java.math.BigDecimal;

public record RefundResponse(String refundId, BigDecimal amount, String currency, String status) {}
