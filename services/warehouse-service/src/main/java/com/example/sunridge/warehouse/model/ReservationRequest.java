package com.example.sunridge.warehouse.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReservationRequest(@NotNull Long orderId, @NotEmpty List<@Valid Item> items) {
  public record Item(@NotNull Long productId, @Min(1) int quantity) {}
}
