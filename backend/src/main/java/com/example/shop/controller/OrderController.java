package com.example.shop.controller;

import com.example.shop.dto.OrderResponse;
import com.example.shop.service.OrderService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
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

  private Long currentUserId(Authentication authentication) {
    return Long.valueOf(authentication.getName());
  }
}
