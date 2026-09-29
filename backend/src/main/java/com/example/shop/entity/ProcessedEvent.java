package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
  @Id
  @Column(length = 64)
  private String eventId;

  public ProcessedEvent() {}

  public ProcessedEvent(String eventId) {
    this.eventId = eventId;
  }
}
