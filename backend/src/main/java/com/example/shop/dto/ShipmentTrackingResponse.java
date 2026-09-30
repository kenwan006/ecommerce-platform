package com.example.shop.dto;

import java.time.Instant;
import java.util.List;

public record ShipmentTrackingResponse(
    Long orderId,
    String carrier,
    String trackingNumber,
    String status,
    List<Event> events) {
  public record Event(
      Long id,
      String providerEventId,
      String status,
      String location,
      String description,
      Instant occurredAt) {}
}
