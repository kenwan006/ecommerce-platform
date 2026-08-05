package com.example.shop.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record OrderRequest(
    @NotNull Long userId,
    @NotBlank String paymentIntentId,
    @NotEmpty List<@Valid Item> items) {
  public record Item(@NotNull Long productId, @Min(1) int quantity) {}
}
