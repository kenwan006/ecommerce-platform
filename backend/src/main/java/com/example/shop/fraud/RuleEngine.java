package com.example.shop.fraud;

import com.example.shop.fraud.model.FraudFeatures;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {
  public List<String> evaluate(FraudFeatures features) {
    List<String> reasons = new ArrayList<>();
    if (features.orderTotal().compareTo(new BigDecimal("500.00")) >= 0) {
      reasons.add("HIGH_ORDER_AMOUNT");
    }
    if (features.recentCheckoutCount() >= 5) {
      reasons.add("CHECKOUT_VELOCITY_HIGH");
    }
    if (features.newAccount()) {
      reasons.add("NEW_ACCOUNT");
    }
    return reasons;
  }
}
