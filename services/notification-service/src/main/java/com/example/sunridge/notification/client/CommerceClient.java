package com.example.sunridge.notification.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CommerceClient {
  private final RestClient restClient;

  public CommerceClient(
      @Value("${services.commerce.base-url}") String baseUrl,
      @Value("${services.internal-api-key}") String internalApiKey) {
    restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
  }

  public OrderSummary order(Long orderId) {
    return restClient
        .get()
        .uri("/internal/notifications/orders/{orderId}", orderId)
        .retrieve()
        .body(OrderSummary.class);
  }

  public record OrderSummary(
      Long orderId, String customerName, String customerEmail, BigDecimal total, String currency) {}
}
