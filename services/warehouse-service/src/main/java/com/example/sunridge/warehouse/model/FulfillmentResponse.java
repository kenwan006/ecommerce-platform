package com.example.sunridge.warehouse.model;

import com.example.sunridge.warehouse.entity.FulfillmentOrder;
import java.util.List;

public record FulfillmentResponse(Long id, Long orderId, String status, List<Item> items) {
  public record Item(String productName, int quantity) {}

  public static FulfillmentResponse from(FulfillmentOrder order) {
    return new FulfillmentResponse(
        order.getId(),
        order.getCommerceOrderId(),
        order.getStatus(),
        order.getItems().stream()
            .map(item -> new Item(item.getProductName(), item.getQuantity()))
            .toList());
  }
}
