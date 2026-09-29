package com.example.sunridge.warehouse.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustmentRequest(
    @NotNull Long productId, @Min(1) int quantity, @NotBlank String type, String note) {}
