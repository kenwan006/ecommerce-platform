package com.example.shop.service;

import com.example.shop.client.PaymentClient;
import com.example.shop.client.WarehouseClient;
import com.example.shop.entity.Order;
import com.example.shop.order.OrderEvent;
import com.example.shop.order.OrderStateMachine;
import com.example.shop.order.OrderStatus;
import com.example.shop.repository.OrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes abandoned checkouts safely. Stripe is consulted immediately before inventory is released
 * because browser payment confirmation and the expiry timer may overlap.
 */
@Service
public class CheckoutExpirationService {
  private static final Logger log = LoggerFactory.getLogger(CheckoutExpirationService.class);

  private final OrderRepository orders;
  private final OrderStateMachine orderStateMachine;
  private final PaymentClient paymentClient;
  private final WarehouseClient warehouseClient;
  private final long checkoutWindowSeconds;

  public CheckoutExpirationService(
      OrderRepository orders,
      OrderStateMachine orderStateMachine,
      PaymentClient paymentClient,
      WarehouseClient warehouseClient,
      @Value("${checkout.expiration.window-seconds:900}") long checkoutWindowSeconds) {
    this.orders = orders;
    this.orderStateMachine = orderStateMachine;
    this.paymentClient = paymentClient;
    this.warehouseClient = warehouseClient;
    this.checkoutWindowSeconds = checkoutWindowSeconds;
  }

  @Scheduled(fixedDelayString = "${checkout.expiration.fixed-delay-ms:60000}")
  @Transactional
  public void expireIncompleteCheckouts() {
    Instant cutoff = Instant.now().minus(checkoutWindowSeconds, ChronoUnit.SECONDS);
    orders
        .findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus.PENDING_PAYMENT, cutoff)
        .forEach(
            order -> {
              try {
                expire(order.getId());
              } catch (Exception exception) {
                log.warn("Could not expire checkout for order {}", order.getId(), exception);
              }
            });
  }

  @Transactional
  public void expire(Long orderId) {
    Order order = orders.findById(orderId).orElseThrow();
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
      return;
    }

    if (order.getStripePaymentIntentId() == null) {
      cancelAndRelease(order);
      return;
    }

    PaymentClient.CheckoutPaymentResolution resolution = paymentClient.expire(order.getCheckoutId());
    switch (resolution.outcome()) {
      case "CANCELED" -> cancelAndRelease(order);
      case "SUCCEEDED", "PROCESSING" -> {
        // A submitted payment remains authoritative. Keep the reservation and await the event.
      }
      default -> throw new IllegalStateException("Unexpected payment expiry outcome");
    }
  }

  private void cancelAndRelease(Order order) {
    warehouseClient.release(order.getId());
    orderStateMachine.transition(order, OrderEvent.CANCELLED, "checkout-expired");
    order.setPaymentStatus("CANCELED");
    order.setStockReserved(false);
    orders.save(order);
  }
}
