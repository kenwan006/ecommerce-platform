package com.example.shop.messaging;

public record PaymentEvent(String eventId, String type, Long orderId, String paymentId) {}
