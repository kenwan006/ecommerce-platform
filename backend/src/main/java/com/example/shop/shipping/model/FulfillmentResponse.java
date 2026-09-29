package com.example.shop.shipping.model;

import com.example.shop.entity.FulfillmentOrder;
import java.math.BigDecimal;
import java.util.List;

public record FulfillmentResponse(
    Long id,
    Long orderId,
    BigDecimal orderTotal,
    String status,
    List<Item> items) {
  public record Item(String productName, int quantity) {}

  public static FulfillmentResponse from(FulfillmentOrder fulfillment) {
    return new FulfillmentResponse(
        fulfillment.getId(),
        fulfillment.getOrder().getId(),
        fulfillment.getOrder().getTotal(),
        fulfillment.getStatus(),
        fulfillment.getItems().stream()
            .map(item -> new Item(item.getProduct().getName(), item.getQuantity()))
            .toList());
  }
}
