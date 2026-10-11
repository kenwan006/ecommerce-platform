package com.example.shop.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {
  private final RestClient restClient;

  public PaymentClient(
      @Value("${services.payment.base-url:http://localhost:8083}") String baseUrl,
      @Value("${services.internal-api-key}") String internalApiKey) {
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
  }

  public Payment create(Long orderId, String checkoutId, BigDecimal amount, String currency) {
    return restClient.post().uri("/internal/payments").contentType(MediaType.APPLICATION_JSON)
        .body(new CreatePayment(orderId, checkoutId, amount, currency)).retrieve().body(Payment.class);
  }

  public Refund refund(Long orderId, Long returnId) {
    return restClient.post().uri(builder -> builder
            .path("/internal/payments/{orderId}/refund")
            .queryParam("returnId", returnId)
            .build(orderId))
        .retrieve().body(Refund.class);
  }

  public CheckoutPaymentResolution expire(String checkoutId) {
    return restClient
        .post()
        .uri("/internal/payments/checkout/{checkoutId}/expire", checkoutId)
        .retrieve()
        .body(CheckoutPaymentResolution.class);
  }

  private record CreatePayment(Long orderId, String checkoutId, BigDecimal amount, String currency) {}
  public record Payment(Long paymentId, String providerPaymentId, String clientSecret, BigDecimal amount, String currency, String status) {}
  public record Refund(String refundId, BigDecimal amount, String currency, String status) {}
  public record CheckoutPaymentResolution(String outcome) {}
}
