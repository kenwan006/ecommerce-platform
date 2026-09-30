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
      @Value("${services.payment.base-url:http://localhost:8083}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public Payment create(Long orderId, String checkoutId, BigDecimal amount, String currency) {
    return restClient.post().uri("/internal/payments").contentType(MediaType.APPLICATION_JSON)
        .body(new CreatePayment(orderId, checkoutId, amount, currency)).retrieve().body(Payment.class);
  }

  private record CreatePayment(Long orderId, String checkoutId, BigDecimal amount, String currency) {}
  public record Payment(Long paymentId, String providerPaymentId, String clientSecret, BigDecimal amount, String currency, String status) {}
}
