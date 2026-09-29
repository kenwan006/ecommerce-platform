package com.example.sunridge.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id
  @Column(columnDefinition = "char(36)")
  private String id;
  private String aggregateType;
  private String aggregateId;
  private String eventType;

  @Column(columnDefinition = "json")
  private String payload;

  private Instant occurredAt;
  private Instant publishedAt;

  public String getId() {
    return id;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }
}
