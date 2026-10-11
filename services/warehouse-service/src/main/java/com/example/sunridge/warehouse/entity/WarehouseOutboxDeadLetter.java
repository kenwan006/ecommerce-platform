package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "warehouse_outbox_dead_letters")
public class WarehouseOutboxDeadLetter {
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

  public static WarehouseOutboxDeadLetter from(WarehouseOutboxEvent event, String error) {
    WarehouseOutboxDeadLetter deadLetter = new WarehouseOutboxDeadLetter();
    deadLetter.outboxEventId = event.getId();
    deadLetter.eventType = event.getEventType();
    deadLetter.aggregateId = event.getAggregateId();
    deadLetter.payload = event.getPayload();
    deadLetter.lastError = error;
    deadLetter.retryCount = event.getRetryCount();
    return deadLetter;
  }
}
