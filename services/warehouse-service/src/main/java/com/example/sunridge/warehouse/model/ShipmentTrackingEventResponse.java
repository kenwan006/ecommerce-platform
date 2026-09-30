package com.example.sunridge.warehouse.model;

import com.example.sunridge.warehouse.entity.ShipmentTrackingEvent;
import java.time.Instant;

public record ShipmentTrackingEventResponse(
    Long id,
    String providerEventId,
    String status,
    String location,
    String description,
    Instant occurredAt) {
  public static ShipmentTrackingEventResponse from(ShipmentTrackingEvent event) {
    return new ShipmentTrackingEventResponse(
        event.getId(),
        event.getProviderEventId(),
        event.getStatus(),
        event.getLocation(),
        event.getDescription(),
        event.getOccurredAt());
  }
}
