package com.example.shop.fraud.model;

import java.math.BigDecimal;

public record FraudFeatures(
    Long orderId,
    Long userId,
    BigDecimal orderTotal,
    long recentCheckoutCount,
    boolean newAccount) {}
