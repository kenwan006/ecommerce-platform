package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
public class Return {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @Column(name = "rma_number")
  private String rmaNumber;

  private String status;
  private String reason;
  private String carrier;

  @Column(name = "tracking_number")
  private String trackingNumber;

  @Column(name = "label_url")
  private String labelUrl;

  @Column(name = "inspection_note")
  private String inspectionNote;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "received_at")
  private Instant receivedAt;

  @Column(name = "refunded_at")
  private Instant refundedAt;

  @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReturnItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public Order getOrder() { return order; }
  public String getRmaNumber() { return rmaNumber; }
  public String getStatus() { return status; }
  public String getReason() { return reason; }
  public String getCarrier() { return carrier; }
  public String getTrackingNumber() { return trackingNumber; }
  public String getLabelUrl() { return labelUrl; }
  public String getInspectionNote() { return inspectionNote; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getReceivedAt() { return receivedAt; }
  public Instant getRefundedAt() { return refundedAt; }
  public List<ReturnItem> getItems() { return items; }

  public void setOrder(Order order) { this.order = order; }
  public void setRmaNumber(String rmaNumber) { this.rmaNumber = rmaNumber; }
  public void setStatus(String status) { this.status = status; }
  public void setReason(String reason) { this.reason = reason; }
  public void setCarrier(String carrier) { this.carrier = carrier; }
  public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
  public void setLabelUrl(String labelUrl) { this.labelUrl = labelUrl; }
  public void setInspectionNote(String inspectionNote) { this.inspectionNote = inspectionNote; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
  public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }
  public void addItem(ReturnItem item) { items.add(item); item.setReturnRequest(this); }
}
