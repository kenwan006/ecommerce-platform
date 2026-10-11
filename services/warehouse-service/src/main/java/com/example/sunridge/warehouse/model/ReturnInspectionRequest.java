package com.example.sunridge.warehouse.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReturnInspectionRequest(
    @NotNull Long returnId,
    @NotNull Long orderId,
    @NotBlank String disposition,
    String inspectionNote,
    @NotEmpty List<@Valid Item> items) {
  public record Item(@NotNull Long productId, @Min(1) int quantity) {}
}
