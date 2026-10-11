package com.example.sunridge.notification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String providerEventId;
  private String eventType;
  private Long commerceOrderId;
  private String recipient;
  private String subject;
  private String status;
  private Instant createdAt = Instant.now();
  private Instant sentAt;

  public static NotificationDelivery sent(
      String eventId, String eventType, Long orderId, String recipient, String subject) {
    NotificationDelivery delivery = new NotificationDelivery();
    delivery.providerEventId = eventId;
    delivery.eventType = eventType;
    delivery.commerceOrderId = orderId;
    delivery.recipient = recipient;
    delivery.subject = subject;
    delivery.status = "SENT";
    delivery.sentAt = Instant.now();
    return delivery;
  }
}
