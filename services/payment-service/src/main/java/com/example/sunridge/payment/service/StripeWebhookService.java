package com.example.sunridge.payment.service;

import com.example.sunridge.payment.entity.OutboxEvent;
import com.example.sunridge.payment.entity.PaymentRefund;
import com.example.sunridge.payment.entity.PaymentWebhookEvent;
import com.example.sunridge.payment.repository.OutboxEventRepository;
import com.example.sunridge.payment.repository.PaymentRefundRepository;
import com.example.sunridge.payment.repository.PaymentRepository;
import com.example.sunridge.payment.repository.PaymentWebhookEventRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {
  private final PaymentRepository payments;
  private final PaymentRefundRepository paymentRefunds;
  private final OutboxEventRepository outbox;
  private final PaymentWebhookEventRepository webhookEvents;

  public StripeWebhookService(
      PaymentRepository payments,
      PaymentRefundRepository paymentRefunds,
      OutboxEventRepository outbox,
      PaymentWebhookEventRepository webhookEvents) {
    this.payments = payments;
    this.paymentRefunds = paymentRefunds;
    this.outbox = outbox;
    this.webhookEvents = webhookEvents;
  }

  @Transactional
  public void handle(Event event) {
    if (webhookEvents.existsByProviderEventId(event.getId())) return;

    switch (event.getType()) {
      case "payment_intent.succeeded" -> handlePaymentIntent(event, true);
      case "payment_intent.payment_failed", "payment_intent.canceled" ->
          handlePaymentIntent(event, false);
      case "refund.updated" -> handleRefund(event);
      default -> {
        recordWebhookEvent(event, "IGNORED");
        return;
      }
    }
    recordWebhookEvent(event, "PROCESSED");
  }

  private void handlePaymentIntent(Event event, boolean succeeded) {
    StripeObject object = deserialize(event);
    if (!(object instanceof PaymentIntent intent))
      throw new IllegalArgumentException("Expected PaymentIntent");
    var payment =
        payments
            .findByProviderPaymentId(intent.getId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    if (succeeded) {
      markPaymentSucceeded(payment, event.getId());
    } else {
      markPaymentFailed(payment, event.getId());
    }
  }

  /**
   * Applies Stripe's current PaymentIntent state during scheduled reconciliation.
   *
   * <p>It is intentionally shared with webhook processing so both paths emit the identical
   * outbox event and state transition.
   */
  public void reconcilePaymentIntent(PaymentIntent intent, String reconciliationEventId) {
    var payment =
        payments
            .findByProviderPaymentId(intent.getId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    if ("succeeded".equals(intent.getStatus())) {
      markPaymentSucceeded(payment, reconciliationEventId);
    } else if ("processing".equals(intent.getStatus()) && isOpen(payment)) {
      payment.setStatus("PROCESSING");
    } else if ("canceled".equals(intent.getStatus())) {
      markPaymentFailed(payment, reconciliationEventId);
    }
  }

  private void markPaymentSucceeded(
      com.example.sunridge.payment.entity.Payment payment, String sourceEventId) {
    if (!isOpen(payment)) return;
    payment.setStatus("PAID");
    publishPaymentOutcome(payment, sourceEventId, "PaymentSucceeded");
  }

  private void markPaymentFailed(
      com.example.sunridge.payment.entity.Payment payment, String sourceEventId) {
    if (!isOpen(payment)) return;
    payment.setStatus("FAILED");
    publishPaymentOutcome(payment, sourceEventId, "PaymentFailed");
  }

  private boolean isOpen(com.example.sunridge.payment.entity.Payment payment) {
    return "CREATED".equals(payment.getStatus())
        || "PENDING".equals(payment.getStatus())
        || "PROCESSING".equals(payment.getStatus());
  }

  private void publishPaymentOutcome(
      com.example.sunridge.payment.entity.Payment payment, String sourceEventId, String eventType) {
    OutboxEvent message = new OutboxEvent();
    message.setId(UUID.randomUUID().toString());
    message.setAggregateType("Payment");
    message.setAggregateId(payment.getId().toString());
    message.setEventType(eventType);
    message.setOccurredAt(Instant.now());
    message.setPayload(
        "{\"eventId\":\""
            + sourceEventId
            + "\",\"type\":\""
            + eventType
            + "\",\"orderId\":"
            + payment.getCommerceOrderId()
            + ",\"paymentId\":\""
            + payment.getProviderPaymentId()
            + "\"}");
    outbox.save(message);
  }

  private void handleRefund(Event event) {
    StripeObject object = deserialize(event);
    if (!(object instanceof Refund refund)) throw new IllegalArgumentException("Expected Refund");
    if (!"succeeded".equals(refund.getStatus()) && !"failed".equals(refund.getStatus())) return;

    var paymentRefund =
        paymentRefunds
            .findByProviderRefundId(refund.getId())
            .orElseThrow(() -> new IllegalArgumentException("Refund not found"));
    if (!"PENDING".equals(paymentRefund.getStatus())) return;

    boolean succeeded = "succeeded".equals(refund.getStatus());
    String eventType = succeeded ? "RefundSucceeded" : "RefundFailed";
    paymentRefund.setStatus(succeeded ? "SUCCEEDED" : "FAILED");
    if (succeeded) {
      paymentRefund.setCompletedAt(Instant.now());
      var payment = paymentRefund.getPayment();
      payment.setProviderRefundId(refund.getId());
      payment.setStatus("REFUNDED");
    }
    publishRefundOutcome(event, paymentRefund, eventType);
  }

  private StripeObject deserialize(Event event) {
    StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
    if (object != null) return object;
    try {
      return event.getDataObjectDeserializer().deserializeUnsafe();
    } catch (EventDataObjectDeserializationException exception) {
      throw new IllegalArgumentException("Cannot deserialize Stripe event", exception);
    }
  }

  private void publishRefundOutcome(
      Event event, PaymentRefund paymentRefund, String eventType) {
    OutboxEvent message = new OutboxEvent();
    message.setId(UUID.randomUUID().toString());
    message.setAggregateType("Refund");
    message.setAggregateId(paymentRefund.getProviderRefundId());
    message.setEventType(eventType);
    message.setOccurredAt(Instant.now());
    message.setPayload(
        "{\"eventId\":\""
            + event.getId()
            + "\",\"type\":\""
            + eventType
            + "\",\"orderId\":"
            + paymentRefund.getCommerceOrderId()
            + ",\"paymentId\":\""
            + paymentRefund.getProviderRefundId()
            + "\"}");
    outbox.save(message);
  }

  private void recordWebhookEvent(Event event, String processingStatus) {
    PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent();
    webhookEvent.setProvider("STRIPE");
    webhookEvent.setProviderEventId(event.getId());
    webhookEvent.setEventType(event.getType());
    webhookEvent.setPayload("{\"eventId\":\"" + event.getId() + "\"}");
    webhookEvent.setProcessingStatus(processingStatus);
    webhookEvents.save(webhookEvent);
  }
}
