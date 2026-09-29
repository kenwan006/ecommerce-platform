package com.example.shop.inventory.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
    @NotNull Long productId,
    @Min(1) int quantity,
    @NotNull InventoryMovementType type,
    @Size(max = 1000) String note) {}
