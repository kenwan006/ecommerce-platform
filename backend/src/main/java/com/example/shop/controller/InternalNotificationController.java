package com.example.shop.controller;

import com.example.shop.dto.NotificationOrderResponse;
import com.example.shop.entity.Order;
import com.example.shop.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
  private final OrderRepository orders;

  public InternalNotificationController(OrderRepository orders) {
    this.orders = orders;
  }

  @GetMapping("/orders/{orderId}")
  @Transactional(readOnly = true)
  public NotificationOrderResponse order(@PathVariable Long orderId) {
    Order order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    return new NotificationOrderResponse(
        order.getId(), order.getUser().getName(), order.getUser().getEmail(), order.getTotal(), "USD");
  }
}
