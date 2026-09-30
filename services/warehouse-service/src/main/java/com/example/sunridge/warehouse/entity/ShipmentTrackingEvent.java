package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipment_tracking_events")
public class ShipmentTrackingEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "shipment_id")
  private Shipment shipment;

  @Column(name = "provider_event_id")
  private String providerEventId;

  private String status;
  private String location;
  private String description;
  private Instant occurredAt;

  public Long getId() {
    return id;
  }

  public String getProviderEventId() {
    return providerEventId;
  }

  public String getStatus() {
    return status;
  }

  public String getLocation() {
    return location;
  }

  public String getDescription() {
    return description;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setShipment(Shipment shipment) {
    this.shipment = shipment;
  }

  public void setProviderEventId(String providerEventId) {
    this.providerEventId = providerEventId;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
