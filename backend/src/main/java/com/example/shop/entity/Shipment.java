package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "fulfillment_order_id", unique = true)
  private FulfillmentOrder fulfillmentOrder;

  @Column(nullable = false)
  private String carrier;

  @Column(name = "tracking_number", nullable = false, unique = true)
  private String trackingNumber;

  @Column(nullable = false)
  private String status = "SHIPPED";

  @Column(name = "shipped_at", nullable = false)
  private Instant shippedAt = Instant.now();

  public Long getId() { return id; }
  public String getCarrier() { return carrier; }
  public String getTrackingNumber() { return trackingNumber; }
  public String getStatus() { return status; }
  public void setFulfillmentOrder(FulfillmentOrder fulfillmentOrder) { this.fulfillmentOrder = fulfillmentOrder; }
  public void setCarrier(String carrier) { this.carrier = carrier; }
  public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
}
