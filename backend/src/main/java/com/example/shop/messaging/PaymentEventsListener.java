package com.example.shop.messaging;

import com.example.shop.entity.Order;
import com.example.shop.entity.ProcessedEvent;
import com.example.shop.order.OrderEvent;
import com.example.shop.order.OrderStateMachine;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProcessedEventRepository;
import com.example.shop.repository.ReturnRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventsListener {
  private final ObjectMapper objectMapper;
  private final OrderRepository orderRepository;
  private final ProcessedEventRepository processedEventRepository;
  private final ReturnRepository returnRepository;
  private final OrderStateMachine orderStateMachine;

  public PaymentEventsListener(
      ObjectMapper objectMapper,
      OrderRepository orderRepository,
      ProcessedEventRepository processedEventRepository,
      ReturnRepository returnRepository,
      OrderStateMachine orderStateMachine) {
    this.objectMapper = objectMapper;
    this.orderRepository = orderRepository;
    this.processedEventRepository = processedEventRepository;
    this.returnRepository = returnRepository;
    this.orderStateMachine = orderStateMachine;
  }

  @KafkaListener(topics = "payment-events", groupId = "commerce-service")
  @Transactional
  public void handle(String payload) {
    PaymentEvent event = parse(payload);
    if (processedEventRepository.existsById(event.eventId())) return;

    Order order =
        orderRepository
            .findById(event.orderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.orderId()));
    if ("PaymentSucceeded".equals(event.type())) {
      orderStateMachine.transition(order, OrderEvent.PAYMENT_SUCCEEDED, event.paymentId());
      order.setPaymentStatus("PAID");
      order.setStripePaymentIntentId(event.paymentId());
    } else if ("PaymentFailed".equals(event.type())) {
      orderStateMachine.transition(order, OrderEvent.PAYMENT_FAILED, event.paymentId());
      order.setPaymentStatus("FAILED");
    } else if ("RefundSucceeded".equals(event.type())) {
      orderStateMachine.transition(order, OrderEvent.REFUND_SUCCEEDED, event.paymentId());
      order.setPaymentStatus("REFUNDED");
      returnRepository.findByOrderId(order.getId()).ifPresent(returnRequest -> {
        returnRequest.setStatus("REFUNDED");
        returnRequest.setRefundedAt(java.time.Instant.now());
      });
    } else if ("RefundFailed".equals(event.type())) {
      returnRepository.findByOrderId(order.getId())
          .ifPresent(returnRequest -> returnRequest.setStatus("REFUND_FAILED"));
    } else {
      throw new IllegalArgumentException("Unsupported payment event type: " + event.type());
    }
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
  }

  private PaymentEvent parse(String payload) {
    try {
      return objectMapper.readValue(payload, PaymentEvent.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Invalid payment event payload", exception);
    }
  }
}
