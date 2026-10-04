package com.example.shop.controller;

import com.example.shop.dto.OrderResponse;
import com.example.shop.dto.RefundResponse;
import com.example.shop.dto.ShipmentTrackingResponse;
import com.example.shop.client.ShipmentClient;
import com.example.shop.service.OrderService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;
  private final ShipmentClient shipmentClient;

  public OrderController(OrderService service, ShipmentClient shipmentClient) {
    this.service = service;
    this.shipmentClient = shipmentClient;
  }

  @GetMapping
  public List<OrderResponse> history(Authentication authentication) {
    return service.byUser(currentUserId(authentication));
  }

  @GetMapping("/pending-payments")
  public List<OrderResponse> pendingPayments(Authentication authentication) {
    return service.pendingPaymentsByUser(currentUserId(authentication));
  }

  @GetMapping("/{orderId}")
  public OrderResponse getOrder(@PathVariable Long orderId, Authentication authentication) {
    return service.byIdForUser(orderId, currentUserId(authentication));
  }

  @GetMapping("/{orderId}/shipment")
  public ShipmentTrackingResponse shipment(
      @PathVariable Long orderId, Authentication authentication) {
    service.requireOrderOwnership(orderId, currentUserId(authentication));
    return shipmentClient.findByOrderId(orderId);
  }

  @PostMapping("/{orderId}/refund")
  public RefundResponse refund(@PathVariable Long orderId, Authentication authentication) {
    return service.refund(orderId, currentUserId(authentication));
  }

  private Long currentUserId(Authentication authentication) {
    return Long.valueOf(authentication.getName());
  }
}
