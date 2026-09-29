package com.example.shop.fraud;

import com.example.shop.fraud.model.FraudFeatures;
import org.springframework.stereotype.Component;

/**
 * Local demo model. Its logistic-regression coefficients are intentionally visible and are not
 * production fraud calibration.
 *
 * <p>In production, this class would be replaced by an HTTP/gRPC client that sends a versioned
 * feature payload to a separately deployed model-serving API, for example
 * {@code POST /v1/fraud-scores}, and receives a risk score plus model version. The client should
 * use a short timeout, circuit breaker, and an explicit fallback policy when the model service is
 * unavailable.</p>
 */
@Component
public class LogisticRegressionMlClient implements MlClient {
  @Override
  public double score(FraudFeatures features) {
    double normalizedAmount = Math.min(features.orderTotal().doubleValue() / 500.0, 3.0);
    double newAccount = features.newAccount() ? 1.0 : 0.0;
    double velocity = Math.min(features.recentCheckoutCount(), 10) / 5.0;
    double logOdds = -3.0 + (1.3 * normalizedAmount) + (0.9 * newAccount) + (1.2 * velocity);
    return 1.0 / (1.0 + Math.exp(-logOdds));
  }

  @Override
  public String modelVersion() {
    return "demo-logistic-v1";
  }
}
