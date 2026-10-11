package com.example.sunridge.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Durable operator-visible record for an outbox event that exhausted delivery retries. */
@Entity
@Table(name = "outbox_dead_letters")
public class OutboxDeadLetter {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "char(36)")
  private String outboxEventId;
  private String eventType;
  private String aggregateId;

  @Column(columnDefinition = "json")
  private String payload;

  @Column(columnDefinition = "text")
  private String lastError;

  private int retryCount;
  private Instant deadLetteredAt = Instant.now();

  public static OutboxDeadLetter from(OutboxEvent event, String error) {
    OutboxDeadLetter deadLetter = new OutboxDeadLetter();
    deadLetter.outboxEventId = event.getId();
    deadLetter.eventType = event.getEventType();
    deadLetter.aggregateId = event.getAggregateId();
    deadLetter.payload = event.getPayload();
    deadLetter.lastError = error;
    deadLetter.retryCount = event.getRetryCount();
    return deadLetter;
  }
}
