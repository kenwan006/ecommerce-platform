package com.example.sunridge.warehouse.model;

import java.time.Instant;

public record InventoryMovementResponse(
    Long id, String productName, String type, int quantity, String note, Instant createdAt) {}
