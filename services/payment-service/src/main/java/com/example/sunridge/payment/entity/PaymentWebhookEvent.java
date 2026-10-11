package com.example.sunridge.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Inbox/deduplication record for Stripe webhook deliveries.
 *
 * <p>The unique provider event ID makes Stripe's at-least-once delivery safe to process.
 */
@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String provider;

  @Column(name = "provider_event_id")
  private String providerEventId;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "received_at")
  private Instant receivedAt = Instant.now();

  @Column(columnDefinition = "json")
  private String payload;

  @Column(name = "processing_status")
  private String processingStatus;

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public void setProviderEventId(String providerEventId) {
    this.providerEventId = providerEventId;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setProcessingStatus(String processingStatus) {
    this.processingStatus = processingStatus;
  }
}
