package com.example.sunridge.warehouse.model;

import jakarta.validation.constraints.NotBlank;

public record ShipmentRequest(@NotBlank String carrier, @NotBlank String trackingNumber) {}
