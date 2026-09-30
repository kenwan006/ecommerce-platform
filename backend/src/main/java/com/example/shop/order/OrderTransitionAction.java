package com.example.shop.order;

import com.example.shop.entity.Order;

@FunctionalInterface
public interface OrderTransitionAction {
  void execute(
      Order order,
      OrderStatus previousStatus,
      OrderStatus nextStatus,
      OrderEvent event,
      String reference);
}
