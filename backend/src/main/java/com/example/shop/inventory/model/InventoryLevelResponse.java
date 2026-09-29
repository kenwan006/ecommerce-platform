package com.example.shop.inventory.model;

import com.example.shop.entity.InventoryLevel;

public record InventoryLevelResponse(
    Long productId,
    String productName,
    int onHandQuantity,
    int reservedQuantity,
    int availableQuantity) {
  public static InventoryLevelResponse from(InventoryLevel level) {
    return new InventoryLevelResponse(
        level.getProduct().getId(),
        level.getProduct().getName(),
        level.getOnHandQuantity(),
        level.getReservedQuantity(),
        level.getAvailableQuantity());
  }
}
