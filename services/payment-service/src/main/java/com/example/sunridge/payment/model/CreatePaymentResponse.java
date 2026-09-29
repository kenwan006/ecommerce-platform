package com.example.sunridge.payment.model;

import java.math.BigDecimal;

public record CreatePaymentResponse(
    Long paymentId,
    String providerPaymentId,
    String clientSecret,
    BigDecimal amount,
    String currency,
    String status) {}
