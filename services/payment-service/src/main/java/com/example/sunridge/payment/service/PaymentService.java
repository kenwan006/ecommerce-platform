package com.example.sunridge.payment.service;

import com.example.sunridge.payment.entity.Payment;
import com.example.sunridge.payment.model.CreatePaymentRequest;
import com.example.sunridge.payment.model.CreatePaymentResponse;
import com.example.sunridge.payment.model.RefundResponse;
import com.example.sunridge.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private final PaymentRepository payments;
  private final String secretKey;

  public PaymentService(
      PaymentRepository payments, @Value("${stripe.secret-key}") String secretKey) {
    this.payments = payments;
    this.secretKey = secretKey;
  }

  @Transactional
  public CreatePaymentResponse create(CreatePaymentRequest request) {
    Payment existing = payments.findByCheckoutId(request.checkoutId()).orElse(null);
    try {
      if (secretKey.isBlank()) throw new IllegalStateException("STRIPE_SECRET_KEY is required");
      Stripe.apiKey = secretKey;
      if (existing != null) {
        PaymentIntent intent = PaymentIntent.retrieve(existing.getProviderPaymentId());
        return new CreatePaymentResponse(
            existing.getId(),
            existing.getProviderPaymentId(),
            intent.getClientSecret(),
            existing.getAmount(),
            existing.getCurrency(),
            existing.getStatus());
      }
      PaymentIntentCreateParams params =
          PaymentIntentCreateParams.builder()
              .setAmount(request.amount().movePointRight(2).longValueExact())
              .setCurrency(request.currency())
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .build())
              .putMetadata("commerceOrderId", request.orderId().toString())
              .putMetadata("checkoutId", request.checkoutId())
              .build();
      PaymentIntent intent =
          PaymentIntent.create(
              params, RequestOptions.builder().setIdempotencyKey(request.checkoutId()).build());
      Payment payment = new Payment();
      payment.setCommerceOrderId(request.orderId());
      payment.setCheckoutId(request.checkoutId());
      payment.setProvider("STRIPE");
      payment.setProviderPaymentId(intent.getId());
      payment.setAmount(request.amount());
      payment.setCurrency(request.currency());
      payment.setStatus("PENDING");
      payments.save(payment);
      return new CreatePaymentResponse(
          payment.getId(),
          intent.getId(),
          intent.getClientSecret(),
          payment.getAmount(),
          payment.getCurrency(),
          payment.getStatus());
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create Stripe payment", exception);
    }
  }

  @Transactional
  public RefundResponse refund(Long commerceOrderId) {
    Payment payment = payments.findByCommerceOrderId(commerceOrderId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    if ("REFUNDED".equals(payment.getStatus())) {
      return new RefundResponse(payment.getProviderRefundId(), payment.getAmount(), payment.getCurrency(), "SUCCEEDED");
    }
    if (!"PAID".equals(payment.getStatus())) {
      throw new IllegalStateException("Only paid payments can be refunded.");
    }
    try {
      if (secretKey.isBlank()) throw new IllegalStateException("STRIPE_SECRET_KEY is required");
      Stripe.apiKey = secretKey;
      Refund refund = Refund.create(
          RefundCreateParams.builder().setPaymentIntent(payment.getProviderPaymentId()).build(),
          RequestOptions.builder().setIdempotencyKey("refund-order-" + commerceOrderId).build());
      if (!"succeeded".equals(refund.getStatus())) {
        throw new IllegalStateException("Stripe refund is " + refund.getStatus());
      }
      payment.setProviderRefundId(refund.getId());
      payment.setStatus("REFUNDED");
      return new RefundResponse(refund.getId(), payment.getAmount(), payment.getCurrency(), "SUCCEEDED");
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not refund Stripe payment", exception);
    }
  }
}
