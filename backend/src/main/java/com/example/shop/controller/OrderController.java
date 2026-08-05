package com.example.shop.controller;

import com.example.shop.dto.OrderResponse;
import com.example.shop.service.OrderService;
import java.util.List;
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

  @GetMapping("/user/{userId}")
  public List<OrderResponse> history(@PathVariable Long userId) {
    return service.byUser(userId);
  }
}
