package com.example.sunridge.payment.service;

import com.example.sunridge.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repairs local payment state when a Stripe webhook is delayed or was not delivered.
 *
 * <p>It checks only nonterminal payments older than a grace period. A PaymentIntent in
 * {@code requires_payment_method} or {@code requires_action} is left open for the customer until
 * the separate checkout-expiration policy closes it.
 */
@Component
public class PaymentReconciliationService {
  private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);
  private static final List<String> OPEN_STATUSES = List.of("CREATED", "PENDING", "PROCESSING");

  private final PaymentRepository payments;
  private final StripeWebhookService paymentState;
  private final String secretKey;
  private final long gracePeriodSeconds;

  public PaymentReconciliationService(
      PaymentRepository payments,
      StripeWebhookService paymentState,
      @Value("${stripe.secret-key}") String secretKey,
      @Value("${payments.reconciliation.grace-period-seconds:60}") long gracePeriodSeconds) {
    this.payments = payments;
    this.paymentState = paymentState;
    this.secretKey = secretKey;
    this.gracePeriodSeconds = gracePeriodSeconds;
  }

  @Scheduled(fixedDelayString = "${payments.reconciliation.fixed-delay-ms:60000}")
  @Transactional
  public void reconcile() {
    if (secretKey.isBlank()) return;
    Stripe.apiKey = secretKey;
    Instant cutoff = Instant.now().minus(gracePeriodSeconds, ChronoUnit.SECONDS);
    payments.findTop100ByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(OPEN_STATUSES, cutoff)
        .forEach(this::refresh);
  }

  void refresh(com.example.sunridge.payment.entity.Payment payment) {
    try {
      PaymentIntent intent = PaymentIntent.retrieve(payment.getProviderPaymentId());
      paymentState.reconcilePaymentIntent(
          intent, "reconcile-payment-intent-" + intent.getId() + "-" + intent.getStatus());
    } catch (Exception exception) {
      log.warn("Could not reconcile Stripe PaymentIntent {}", payment.getProviderPaymentId(), exception);
    }
  }
}
