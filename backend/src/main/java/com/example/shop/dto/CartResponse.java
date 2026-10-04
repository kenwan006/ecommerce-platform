package com.example.shop.dto;

import com.example.shop.entity.Cart;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<Item> items, BigDecimal total) {
  public record Item(
      Long id,
      String name,
      String description,
      BigDecimal price,
      String imageUrl,
      int quantity) {}

  public static CartResponse from(Cart cart) {
    List<Item> items =
        cart.getItems().stream()
            .map(
                item ->
                    new Item(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getDescription(),
                        item.getProduct().getPrice(),
                        item.getProduct().getImageUrl(),
                        item.getQuantity()))
            .toList();
    BigDecimal total =
        items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartResponse(items, total);
  }
}
