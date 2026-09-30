package com.example.sunridge.warehouse.carrier;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record FedExTrackingEvent(
    String eventId,
    @NotBlank String trackingNumber,
    @NotBlank String status,
    String location,
    String description,
    Instant occurredAt) {}
