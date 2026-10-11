package com.example.shop.dto;

import com.example.shop.entity.Return;
import java.time.Instant;
import java.util.List;

public record ReturnResponse(
    Long id,
    Long orderId,
    String rmaNumber,
    String status,
    String reason,
    String carrier,
    String trackingNumber,
    String labelUrl,
    String inspectionNote,
    Instant createdAt,
    Instant receivedAt,
    Instant refundedAt,
    List<Item> items) {
  public record Item(Long productId, String productName, int quantity) {}

  public static ReturnResponse from(Return returnRequest) {
    return new ReturnResponse(
        returnRequest.getId(),
        returnRequest.getOrder().getId(),
        returnRequest.getRmaNumber(),
        returnRequest.getStatus(),
        returnRequest.getReason(),
        returnRequest.getCarrier(),
        returnRequest.getTrackingNumber(),
        returnRequest.getLabelUrl(),
        returnRequest.getInspectionNote(),
        returnRequest.getCreatedAt(),
        returnRequest.getReceivedAt(),
        returnRequest.getRefundedAt(),
        returnRequest.getItems().stream()
            .map(item -> new Item(item.getProductId(), item.getProductName(), item.getQuantity()))
            .toList());
  }
}
