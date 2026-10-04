package com.example.shop.order;

import com.example.shop.entity.Order;
import com.example.shop.entity.OrderStatusHistory;
import com.example.shop.repository.OrderStatusHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Stateless definition of the Commerce order lifecycle.
 *
 * <p>Each transition has a source state, event, guard, target state, and action. Actions are
 * intentionally local and transactional; cross-service work belongs in an outbox event.
 */
@Component
public class OrderStateMachine {
  private final OrderStatusHistoryRepository historyRepository;
  private final List<Transition> transitions;

  public OrderStateMachine(OrderStatusHistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
    OrderTransitionAction recordHistory = this::recordHistory;
    OrderTransitionGuard paymentMatchesOrder =
        (order, paymentId) ->
            order.getStripePaymentIntentId() == null
                || order.getStripePaymentIntentId().equals(paymentId);

    transitions =
        List.of(
            new Transition(
                OrderStatus.PENDING_PAYMENT,
                OrderEvent.PAYMENT_SUCCEEDED,
                OrderStatus.PAID,
                paymentMatchesOrder,
                recordHistory),
            new Transition(
                OrderStatus.PENDING_PAYMENT,
                OrderEvent.PAYMENT_FAILED,
                OrderStatus.PAYMENT_FAILED,
                paymentMatchesOrder,
                recordHistory),
            new Transition(
                OrderStatus.PENDING_PAYMENT,
                OrderEvent.FRAUD_REVIEW_REQUIRED,
                OrderStatus.FRAUD_REVIEW,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.PENDING_PAYMENT,
                OrderEvent.FRAUD_DECLINED,
                OrderStatus.FRAUD_DECLINED,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.FRAUD_REVIEW,
                OrderEvent.REVIEW_APPROVED,
                OrderStatus.PENDING_PAYMENT,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.FRAUD_REVIEW,
                OrderEvent.REVIEW_DECLINED,
                OrderStatus.FRAUD_DECLINED,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.PENDING_PAYMENT,
                OrderEvent.CANCELLED,
                OrderStatus.CANCELLED,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.FRAUD_REVIEW,
                OrderEvent.CANCELLED,
                OrderStatus.CANCELLED,
                (order, reference) -> true,
                recordHistory),
            new Transition(
                OrderStatus.PAID,
                OrderEvent.REFUND_SUCCEEDED,
                OrderStatus.REFUNDED,
                (order, reference) -> true,
                recordHistory));
  }

  public void transition(Order order, OrderEvent event, String reference) {
    OrderStatus current = order.getStatus();
    Transition transition =
        transitions.stream()
            .filter(candidate -> candidate.source() == current && candidate.event() == event)
            .findFirst()
            .orElseThrow(() -> new InvalidOrderTransitionException(current, event));
    if (!transition.guard().allows(order, reference)) {
      throw new InvalidOrderTransitionException(current, event);
    }

    order.setStatus(transition.target());
    transition.action().execute(order, current, transition.target(), event, reference);
  }

  private void recordHistory(
      Order order,
      OrderStatus previousStatus,
      OrderStatus nextStatus,
      OrderEvent event,
      String reference) {
    historyRepository.save(
        new OrderStatusHistory(order, previousStatus, nextStatus, event, reference));
  }

  private record Transition(
      OrderStatus source,
      OrderEvent event,
      OrderStatus target,
      OrderTransitionGuard guard,
      OrderTransitionAction action) {}
}
