package com.example.shop.entity;

import com.example.shop.fraud.model.FraudDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "order_id", unique = true)
  private Order order;

  @Enumerated(EnumType.STRING)
  private FraudDecision decision;

  @Column(name = "risk_score")
  private double riskScore;

  @Column(name = "reasons", nullable = false, length = 1000)
  private String reasons;

  @Column(name = "model_version", nullable = false)
  private String modelVersion;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public FraudDecision getDecision() { return decision; }
  public double getRiskScore() { return riskScore; }
  public String getReasons() { return reasons; }

  public void setOrder(Order order) { this.order = order; }
  public void setDecision(FraudDecision decision) { this.decision = decision; }
  public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
  public void setReasons(String reasons) { this.reasons = reasons; }
  public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
}
