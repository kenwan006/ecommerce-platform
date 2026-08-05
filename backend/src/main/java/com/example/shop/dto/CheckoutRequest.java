package com.example.shop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckoutRequest(
    @NotNull Long userId,
    @NotBlank @Size(max = 255) String checkoutId,
    @NotEmpty List<@Valid Item> items) {
  public record Item(@NotNull Long productId, @jakarta.validation.constraints.Min(1) int quantity) {}
}
