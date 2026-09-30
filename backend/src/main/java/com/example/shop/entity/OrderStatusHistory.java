package com.example.shop.entity;

import com.example.shop.order.OrderEvent;
import com.example.shop.order.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", nullable = false)
  private OrderStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "next_status", nullable = false)
  private OrderStatus nextStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderEvent event;

  @Column(name = "reference_id")
  private String referenceId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt = Instant.now();

  protected OrderStatusHistory() {}

  public OrderStatusHistory(
      Order order,
      OrderStatus previousStatus,
      OrderStatus nextStatus,
      OrderEvent event,
      String referenceId) {
    this.order = order;
    this.previousStatus = previousStatus;
    this.nextStatus = nextStatus;
    this.event = event;
    this.referenceId = referenceId;
  }
}
