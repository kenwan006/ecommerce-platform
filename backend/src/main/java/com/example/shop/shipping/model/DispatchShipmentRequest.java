package com.example.shop.shipping.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DispatchShipmentRequest(
    @NotBlank @Size(max = 100) String carrier,
    @NotBlank @Size(max = 255) String trackingNumber) {}
