package com.example.shop.fraud;

import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudDecisionResult;
import com.example.shop.fraud.model.FraudFeatures;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {
  public FraudDecisionResult decide(FraudFeatures features, double score, List<String> ruleReasons) {
    List<String> reasons = new ArrayList<>(ruleReasons);
    FraudDecision decision;
    if (features.recentCheckoutCount() >= 10 || score >= 0.85) {
      decision = FraudDecision.DECLINE;
      reasons.add("RISK_SCORE_CRITICAL");
    } else if (reasons.contains("HIGH_ORDER_AMOUNT")
        || features.recentCheckoutCount() >= 5 || score >= 0.55) {
      decision = FraudDecision.REVIEW;
      reasons.add("RISK_SCORE_ELEVATED");
    } else {
      decision = FraudDecision.APPROVE;
      reasons.add("RISK_SCORE_ACCEPTABLE");
    }
    return new FraudDecisionResult(decision, score, List.copyOf(reasons));
  }
}
