package com.example.shop.fraud;

import com.example.shop.entity.FraudAssessment;
import com.example.shop.entity.Order;
import com.example.shop.client.WarehouseClient;
import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudDecisionResult;
import com.example.shop.fraud.model.FraudFeatures;
import com.example.shop.repository.FraudAssessmentRepository;
import com.example.shop.repository.OrderRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudAssessmentService {
  private final OrderRepository orderRepository;
  private final FraudAssessmentRepository assessmentRepository;
  private final FeatureService featureService;
  private final RuleEngine ruleEngine;
  private final MlClient mlClient;
  private final DecisionEngine decisionEngine;
  private final WarehouseClient warehouseClient;

  public FraudAssessmentService(
      OrderRepository orderRepository,
      FraudAssessmentRepository assessmentRepository,
      FeatureService featureService,
      RuleEngine ruleEngine,
      MlClient mlClient,
      DecisionEngine decisionEngine,
      WarehouseClient warehouseClient) {
    this.orderRepository = orderRepository;
    this.assessmentRepository = assessmentRepository;
    this.featureService = featureService;
    this.ruleEngine = ruleEngine;
    this.mlClient = mlClient;
    this.decisionEngine = decisionEngine;
    this.warehouseClient = warehouseClient;
  }

  @Transactional
  public FraudDecisionResult assess(Long orderId) {
    FraudAssessment existing = assessmentRepository.findByOrderId(orderId).orElse(null);
    if (existing != null) {
      return new FraudDecisionResult(
          existing.getDecision(), existing.getRiskScore(), splitReasons(existing.getReasons()));
    }

    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found for fraud assessment"));
    FraudFeatures features = featureService.build(order);
    FraudDecisionResult decision = decisionEngine.decide(
        features, mlClient.score(features), ruleEngine.evaluate(features));

    FraudAssessment assessment = new FraudAssessment();
    assessment.setOrder(order);
    assessment.setDecision(decision.decision());
    assessment.setRiskScore(decision.riskScore());
    assessment.setReasons(String.join(",", decision.reasons()));
    assessment.setModelVersion(mlClient.modelVersion());
    assessmentRepository.save(assessment);

    if (decision.decision() == FraudDecision.DECLINE) {
      warehouseClient.release(orderId);
      order.setPaymentStatus("FRAUD_DECLINED");
      order.setStatus("FRAUD_DECLINED");
    } else if (decision.decision() == FraudDecision.REVIEW) {
      order.setStatus("FRAUD_REVIEW");
    }
    return decision;
  }

  private List<String> splitReasons(String reasons) {
    return reasons.isBlank() ? List.of() : Arrays.asList(reasons.split(","));
  }
}
