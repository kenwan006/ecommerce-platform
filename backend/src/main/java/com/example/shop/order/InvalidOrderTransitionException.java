package com.example.shop.order;

public class InvalidOrderTransitionException extends IllegalStateException {
  public InvalidOrderTransitionException(OrderStatus state, OrderEvent event) {
    super("Order cannot transition from " + state + " using event " + event);
  }
}
