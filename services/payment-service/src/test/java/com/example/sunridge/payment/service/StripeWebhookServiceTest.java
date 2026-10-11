package com.example.sunridge.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sunridge.payment.entity.OutboxEvent;
import com.example.sunridge.payment.entity.Payment;
import com.example.sunridge.payment.entity.PaymentRefund;
import com.example.sunridge.payment.entity.PaymentWebhookEvent;
import com.example.sunridge.payment.repository.OutboxEventRepository;
import com.example.sunridge.payment.repository.PaymentRefundRepository;
import com.example.sunridge.payment.repository.PaymentRepository;
import com.example.sunridge.payment.repository.PaymentWebhookEventRepository;
import com.stripe.Stripe;
import com.google.gson.JsonParser;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class StripeWebhookServiceTest {
  @Test
  void reconciliationRepairsCreatedPaymentWhenStripeAlreadySucceeded() {
    Payment payment = new Payment();
    setId(payment, 9L);
    payment.setCommerceOrderId(42L);
    payment.setProviderPaymentId("pi_123");
    payment.setStatus("CREATED");

    List<OutboxEvent> outboxEvents = new ArrayList<>();
    StripeWebhookService service =
        new StripeWebhookService(
            paymentRepository(payment),
            unusedRepository(PaymentRefundRepository.class),
            outboxRepository(outboxEvents),
            unusedRepository(PaymentWebhookEventRepository.class));
    PaymentIntent intent = new PaymentIntent();
    intent.setId("pi_123");
    intent.setStatus("succeeded");

    service.reconcilePaymentIntent(intent, "reconcile-pi_123-succeeded");

    assertThat(payment.getStatus()).isEqualTo("PAID");
    assertThat(outboxEvents).hasSize(1);
    assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PaymentSucceeded");
    assertThat(outboxEvents.get(0).getPayload()).contains("reconcile-pi_123-succeeded");
  }

  @Test
  void refundWebhookCompletesPendingRefundAndPublishesOneOutcome() {
    Payment payment = new Payment();
    payment.setStatus("PAID");

    PaymentRefund refund = new PaymentRefund();
    refund.setPayment(payment);
    refund.setCommerceOrderId(42L);
    refund.setCommerceReturnId(8L);
    refund.setProviderRefundId("re_123");
    refund.setAmount(new BigDecimal("12.00"));
    refund.setCurrency("usd");
    refund.setStatus("PENDING");

    List<OutboxEvent> outboxEvents = new ArrayList<>();
    AtomicBoolean alreadyProcessed = new AtomicBoolean();
    StripeWebhookService service =
        new StripeWebhookService(
            unusedRepository(PaymentRepository.class),
            paymentRefundRepository(refund),
            outboxRepository(outboxEvents),
            webhookEventRepository(alreadyProcessed));

    Event event = refundUpdatedEvent("evt_refund_123", "re_123", "succeeded");
    service.handle(event);
    service.handle(event);

    assertThat(refund.getStatus()).isEqualTo("SUCCEEDED");
    assertThat(payment.getStatus()).isEqualTo("REFUNDED");
    assertThat(payment.getProviderRefundId()).isEqualTo("re_123");
    assertThat(outboxEvents).hasSize(1);
    assertThat(outboxEvents.get(0).getEventType()).isEqualTo("RefundSucceeded");
    assertThat(outboxEvents.get(0).getPayload())
        .contains("evt_refund_123", "RefundSucceeded", "\"orderId\":42");
  }

  private static Event refundUpdatedEvent(String eventId, String refundId, String status) {
    Event event = new Event();
    event.setId(eventId);
    event.setType("refund.updated");
    event.setApiVersion(Stripe.API_VERSION);
    Event.Data data = new Event.Data();
    data.setObject(
        JsonParser.parseString(
                "{\"id\":\""
                    + refundId
                    + "\",\"object\":\"refund\",\"status\":\""
                    + status
                    + "\"}")
            .getAsJsonObject());
    event.setData(data);
    return event;
  }

  private static PaymentRefundRepository paymentRefundRepository(PaymentRefund refund) {
    return (PaymentRefundRepository)
        Proxy.newProxyInstance(
            PaymentRefundRepository.class.getClassLoader(),
            new Class<?>[] {PaymentRefundRepository.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("findByProviderRefundId")) return Optional.of(refund);
              return defaultValue(method.getReturnType());
            });
  }

  private static PaymentRepository paymentRepository(Payment payment) {
    return (PaymentRepository)
        Proxy.newProxyInstance(
            PaymentRepository.class.getClassLoader(),
            new Class<?>[] {PaymentRepository.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("findByProviderPaymentId")) return Optional.of(payment);
              return defaultValue(method.getReturnType());
            });
  }

  private static OutboxEventRepository outboxRepository(List<OutboxEvent> events) {
    return (OutboxEventRepository)
        Proxy.newProxyInstance(
            OutboxEventRepository.class.getClassLoader(),
            new Class<?>[] {OutboxEventRepository.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("save")) {
                events.add((OutboxEvent) arguments[0]);
                return arguments[0];
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static PaymentWebhookEventRepository webhookEventRepository(AtomicBoolean processed) {
    return (PaymentWebhookEventRepository)
        Proxy.newProxyInstance(
            PaymentWebhookEventRepository.class.getClassLoader(),
            new Class<?>[] {PaymentWebhookEventRepository.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("existsByProviderEventId")) return processed.get();
              if (method.getName().equals("save")) {
                processed.set(true);
                return (PaymentWebhookEvent) arguments[0];
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static <T> T unusedRepository(Class<T> type) {
    return type.cast(
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, arguments) -> defaultValue(method.getReturnType())));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == long.class) return 0L;
    if (type == int.class) return 0;
    return null;
  }

  private static void setId(Object target, Long id) {
    try {
      var field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
