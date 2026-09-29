package com.example.shop.inventory.model;

import com.example.shop.entity.InventoryMovement;
import java.time.Instant;

public record InventoryMovementResponse(
    Long id,
    String productName,
    String type,
    int quantity,
    String note,
    Instant createdAt) {
  public static InventoryMovementResponse from(InventoryMovement movement) {
    return new InventoryMovementResponse(
        movement.getId(),
        movement.getProduct().getName(),
        movement.getType(),
        movement.getQuantity(),
        movement.getNote(),
        movement.getCreatedAt());
  }
}
