package com.example.shop.messaging;

import com.example.shop.entity.Order;
import com.example.shop.entity.ProcessedEvent;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProcessedEventRepository;
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

  public PaymentEventsListener(
      ObjectMapper objectMapper,
      OrderRepository orderRepository,
      ProcessedEventRepository processedEventRepository) {
    this.objectMapper = objectMapper;
    this.orderRepository = orderRepository;
    this.processedEventRepository = processedEventRepository;
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
      order.setStatus("PAID");
      order.setPaymentStatus("PAID");
      order.setStripePaymentIntentId(event.paymentId());
    } else if ("PaymentFailed".equals(event.type())) {
      order.setStatus("PAYMENT_FAILED");
      order.setPaymentStatus("FAILED");
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
