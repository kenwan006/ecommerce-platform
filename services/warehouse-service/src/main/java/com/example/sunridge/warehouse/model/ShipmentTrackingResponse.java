package com.example.sunridge.warehouse.model;

import java.util.List;

public record ShipmentTrackingResponse(
    Long orderId,
    String carrier,
    String trackingNumber,
    String status,
    List<ShipmentTrackingEventResponse> events) {}
