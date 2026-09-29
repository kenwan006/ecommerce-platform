package com.example.shop.fraud;

import com.example.shop.entity.Order;
import com.example.shop.fraud.model.FraudFeatures;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class FeatureService {
  private final VelocityService velocityService;

  public FeatureService(VelocityService velocityService) {
    this.velocityService = velocityService;
  }

  public FraudFeatures build(Order order) {
    Instant accountCreatedAt = order.getUser().getCreatedAt();
    boolean newAccount = accountCreatedAt != null
        && accountCreatedAt.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
    return new FraudFeatures(
        order.getId(),
        order.getUser().getId(),
        order.getTotal(),
        velocityService.recentCheckoutCount(order.getUser().getId()),
        newAccount);
  }
}
