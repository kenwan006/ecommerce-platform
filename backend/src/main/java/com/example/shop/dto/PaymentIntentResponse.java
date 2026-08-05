package com.example.shop.dto;

public record PaymentIntentResponse(
    String paymentIntentId,
    String clientSecret,
    long amount,
    String currency) {}
