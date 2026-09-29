package com.example.shop.fraud.model;

import java.util.List;

public record FraudDecisionResult(
    FraudDecision decision,
    double riskScore,
    List<String> reasons) {}
