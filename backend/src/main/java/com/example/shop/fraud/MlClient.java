package com.example.shop.fraud;

import com.example.shop.fraud.model.FraudFeatures;

public interface MlClient {
  double score(FraudFeatures features);

  String modelVersion();
}
