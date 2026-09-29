package com.example.sunridge.payment.service;

import com.example.sunridge.payment.entity.OutboxEvent;
import com.example.sunridge.payment.repository.OutboxEventRepository;
import com.example.sunridge.payment.repository.PaymentRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {
  private final PaymentRepository payments;
  private final OutboxEventRepository outbox;

  public StripeWebhookService(PaymentRepository payments, OutboxEventRepository outbox) {
    this.payments = payments;
    this.outbox = outbox;
  }

  @Transactional
  public void handle(Event event) {
    boolean succeeded = "payment_intent.succeeded".equals(event.getType());
    boolean failed =
        "payment_intent.payment_failed".equals(event.getType())
            || "payment_intent.canceled".equals(event.getType());
    if (!succeeded && !failed) return;
    StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
    if (object == null) {
      try {
        object = event.getDataObjectDeserializer().deserializeUnsafe();
      } catch (EventDataObjectDeserializationException exception) {
        throw new IllegalArgumentException("Cannot deserialize Stripe event", exception);
      }
    }
    if (!(object instanceof PaymentIntent intent))
      throw new IllegalArgumentException("Expected PaymentIntent");
    var payment =
        payments
            .findByProviderPaymentId(intent.getId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    if (!"PENDING".equals(payment.getStatus())) return;
    String eventType = succeeded ? "PaymentSucceeded" : "PaymentFailed";
    payment.setStatus(succeeded ? "PAID" : "FAILED");
    OutboxEvent message = new OutboxEvent();
    message.setId(UUID.randomUUID().toString());
    message.setAggregateType("Payment");
    message.setAggregateId(payment.getId().toString());
    message.setEventType(eventType);
    message.setOccurredAt(Instant.now());
    message.setPayload(
        "{\"eventId\":\""
            + event.getId()
            + "\",\"type\":\""
            + eventType
            + "\",\"orderId\":"
            + payment.getCommerceOrderId()
            + ",\"paymentId\":\""
            + payment.getProviderPaymentId()
            + "\"}");
    outbox.save(message);
  }
}
