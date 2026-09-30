package com.example.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shop.entity.Order;
import com.example.shop.repository.OrderStatusHistoryRepository;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {
  @Test
  void paymentSucceededMovesPendingOrderToPaidAndRecordsHistory() {
    AtomicInteger savedHistoryRows = new AtomicInteger();
    OrderStatusHistoryRepository historyRepository = historyRepository(savedHistoryRows);
    OrderStateMachine stateMachine = new OrderStateMachine(historyRepository);
    Order order = new Order();
    order.setStripePaymentIntentId("pi_123");

    stateMachine.transition(order, OrderEvent.PAYMENT_SUCCEEDED, "pi_123");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(savedHistoryRows).hasValue(1);
  }

  @Test
  void paymentWithAnotherPaymentIdIsRejectedByGuard() {
    OrderStateMachine stateMachine = new OrderStateMachine(historyRepository(new AtomicInteger()));
    Order order = new Order();
    order.setStripePaymentIntentId("pi_expected");

    assertThatThrownBy(
            () -> stateMachine.transition(order, OrderEvent.PAYMENT_SUCCEEDED, "pi_other"))
        .isInstanceOf(InvalidOrderTransitionException.class);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
  }

  @Test
  void paidOrderCannotReceiveAnotherPaymentSucceededEvent() {
    OrderStateMachine stateMachine = new OrderStateMachine(historyRepository(new AtomicInteger()));
    Order order = new Order();

    stateMachine.transition(order, OrderEvent.PAYMENT_SUCCEEDED, "pi_123");

    assertThatThrownBy(
            () -> stateMachine.transition(order, OrderEvent.PAYMENT_SUCCEEDED, "pi_123"))
        .isInstanceOf(InvalidOrderTransitionException.class);
  }

  private static OrderStatusHistoryRepository historyRepository(AtomicInteger savedHistoryRows) {
    return (OrderStatusHistoryRepository)
        Proxy.newProxyInstance(
            OrderStatusHistoryRepository.class.getClassLoader(),
            new Class<?>[] {OrderStatusHistoryRepository.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("save")) {
                savedHistoryRows.incrementAndGet();
                return arguments[0];
              }
              if (method.getReturnType() == boolean.class) {
                return false;
              }
              if (method.getReturnType() == long.class) {
                return 0L;
              }
              return null;
            });
  }
}
