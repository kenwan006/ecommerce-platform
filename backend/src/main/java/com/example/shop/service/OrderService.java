package com.example.shop.service;

import com.example.shop.dto.OrderResponse;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OrderService {
  private final UserRepository users;
  private final OrderRepository orders;

  public OrderService(UserRepository users, OrderRepository orders) {
    this.users = users;
    this.orders = orders;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> byUser(Long userId) {
    return orders.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(OrderResponse::from)
        .toList();
  }
}
