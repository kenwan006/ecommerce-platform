package com.example.shop.messaging;

public record WarehouseEvent(
    String eventId, String type, Long returnId, Long orderId, String disposition) {}
