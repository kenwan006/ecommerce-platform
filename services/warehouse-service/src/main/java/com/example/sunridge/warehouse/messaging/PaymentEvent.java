package com.example.sunridge.warehouse.messaging;

public record PaymentEvent(String eventId, String type, Long orderId, String paymentId) {}
