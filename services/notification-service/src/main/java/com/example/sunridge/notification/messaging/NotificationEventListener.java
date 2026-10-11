package com.example.sunridge.notification.messaging;

import com.example.sunridge.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
  private final ObjectMapper objectMapper;
  private final NotificationService notifications;

  public NotificationEventListener(ObjectMapper objectMapper, NotificationService notifications) {
    this.objectMapper = objectMapper;
    this.notifications = notifications;
  }

  @KafkaListener(topics = "payment-events", groupId = "notification-service")
  public void payment(String payload) {
    JsonNode event = parse(payload);
    if ("PaymentSucceeded".equals(event.path("type").asText())) {
      notifications.paymentSucceeded(event.path("eventId").asText(), event.path("orderId").asLong());
    }
  }

  @KafkaListener(topics = "warehouse-events", groupId = "notification-service")
  public void warehouse(String payload) {
    JsonNode event = parse(payload);
    if ("ShipmentCreated".equals(event.path("type").asText())) {
      notifications.shipmentCreated(
          event.path("eventId").asText(),
          event.path("orderId").asLong(),
          event.path("carrier").asText(),
          event.path("trackingNumber").asText());
    }
  }

  private JsonNode parse(String payload) {
    try {
      return objectMapper.readTree(payload);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid notification event", exception);
    }
  }
}
