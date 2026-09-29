package com.example.shop.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudFeatures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecisionEngineTest {
  private final DecisionEngine decisionEngine = new DecisionEngine();

  @Test
  void approvesLowRiskCheckout() {
    var result = decisionEngine.decide(
        new FraudFeatures(1L, 1L, new BigDecimal("28.00"), 1, false),
        0.12,
        List.of());

    assertThat(result.decision()).isEqualTo(FraudDecision.APPROVE);
  }

  @Test
  void reviewsHighVelocityCheckout() {
    var result = decisionEngine.decide(
        new FraudFeatures(1L, 1L, new BigDecimal("28.00"), 5, false),
        0.20,
        List.of("CHECKOUT_VELOCITY_HIGH"));

    assertThat(result.decision()).isEqualTo(FraudDecision.REVIEW);
  }
}
