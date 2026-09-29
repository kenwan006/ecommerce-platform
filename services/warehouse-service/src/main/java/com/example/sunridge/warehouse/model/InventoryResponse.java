package com.example.sunridge.warehouse.model;

import com.example.sunridge.warehouse.entity.InventoryLevel;

public record InventoryResponse(
    Long productId,
    String productName,
    int onHandQuantity,
    int reservedQuantity,
    int availableQuantity) {
  public static InventoryResponse from(InventoryLevel level) {
    return new InventoryResponse(
        level.getProduct().getProductId(),
        level.getProduct().getName(),
        level.getOnHandQuantity(),
        level.getReservedQuantity(),
        level.availableQuantity());
  }
}
