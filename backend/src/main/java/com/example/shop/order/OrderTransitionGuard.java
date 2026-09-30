package com.example.shop.order;

import com.example.shop.entity.Order;

@FunctionalInterface
public interface OrderTransitionGuard {
  boolean allows(Order order, String reference);
}
