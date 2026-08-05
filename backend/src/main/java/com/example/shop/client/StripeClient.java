package com.example.shop.client;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeClient {
  private final String secretKey;

  public StripeClient(@Value("${stripe.secret-key}") String secretKey) {
    this.secretKey = secretKey;
  }

  public PaymentIntent createPaymentIntent(
      long amountInCents,
      String currency,
      String orderId,
      String userId,
      String idempotencyKey) throws StripeException {
    configureStripe();

    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(amountInCents)
        .setCurrency(currency)
        .setAutomaticPaymentMethods(
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .build())
        .putMetadata("orderId", orderId)
        .putMetadata("userId", userId)
        .putMetadata("checkoutId", idempotencyKey)
        .build();

    RequestOptions options = RequestOptions.builder()
        .setIdempotencyKey(idempotencyKey)
        .build();

    return PaymentIntent.create(params, options);
  }

  public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
    configureStripe();
    return PaymentIntent.retrieve(paymentIntentId);
  }

  private void configureStripe() {
    if (secretKey.isBlank()) {
      throw new IllegalStateException("Stripe is not configured. Set STRIPE_SECRET_KEY first.");
    }

    Stripe.apiKey = secretKey;
  }
}
