package com.example.shop.dto;
import com.example.shop.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id, String status, String paymentStatus, BigDecimal total, Instant createdAt, List<Item> items) {
  public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice) {}

  public static OrderResponse from(Order order) {
    List<Item> items = order.getItems().stream()
        .map(item -> new Item(
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getQuantity(),
            item.getUnitPrice()))
        .toList();

    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getPaymentStatus(),
        order.getTotal(),
        order.getCreatedAt(),
        items);
  }
}
