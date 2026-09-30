package com.example.shop.client;

import com.example.shop.dto.ShipmentTrackingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ShipmentClient {
  private final RestClient restClient;

  public ShipmentClient(@Value("${services.warehouse.base-url:http://localhost:8082}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public ShipmentTrackingResponse findByOrderId(Long orderId) {
    return restClient
        .get()
        .uri("/internal/shipments/order/{orderId}", orderId)
        .retrieve()
        .body(ShipmentTrackingResponse.class);
  }
}
