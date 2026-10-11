package com.example.shop.client;

import com.example.shop.dto.ShipmentTrackingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ShipmentClient {
  private final RestClient restClient;

  public ShipmentClient(
      @Value("${services.warehouse.base-url:http://localhost:8082}") String baseUrl,
      @Value("${services.internal-api-key}") String internalApiKey) {
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
  }

  public ShipmentTrackingResponse findByOrderId(Long orderId) {
    return restClient
        .get()
        .uri("/internal/shipments/order/{orderId}", orderId)
        .retrieve()
        .body(ShipmentTrackingResponse.class);
  }
}
