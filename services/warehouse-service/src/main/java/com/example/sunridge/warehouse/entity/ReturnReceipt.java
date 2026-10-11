package com.example.sunridge.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "return_receipts")
public class ReturnReceipt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "commerce_return_id")
  private Long commerceReturnId;

  @Column(name = "commerce_order_id")
  private Long commerceOrderId;

  private String disposition;

  @Column(name = "inspection_note")
  private String inspectionNote;

  @Column(name = "received_at")
  private Instant receivedAt = Instant.now();

  public void setCommerceReturnId(Long commerceReturnId) { this.commerceReturnId = commerceReturnId; }
  public void setCommerceOrderId(Long commerceOrderId) { this.commerceOrderId = commerceOrderId; }
  public void setDisposition(String disposition) { this.disposition = disposition; }
  public void setInspectionNote(String inspectionNote) { this.inspectionNote = inspectionNote; }
}
