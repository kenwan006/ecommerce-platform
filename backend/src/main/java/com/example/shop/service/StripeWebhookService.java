package com.example.shop.service;

import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Product;
import com.example.shop.repository.OrderRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {
  private final OrderRepository orders;

  public StripeWebhookService(OrderRepository orders) {
    this.orders = orders;
  }

  @Transactional
  public void handle(Event event) {
    boolean succeeded = "payment_intent.succeeded".equals(event.getType());
    boolean failed = "payment_intent.payment_failed".equals(event.getType())
        || "payment_intent.canceled".equals(event.getType());
    if (!succeeded && !failed) {
      return;
    }

    var deserializer = event.getDataObjectDeserializer();
    StripeObject stripeObject = deserializer.getObject().orElse(null);
    if (stripeObject == null) {
      try {
        stripeObject = deserializer.deserializeUnsafe();
      } catch (EventDataObjectDeserializationException exception) {
        throw new IllegalArgumentException("Stripe event cannot be deserialized", exception);
      }
    }
    if (!(stripeObject instanceof PaymentIntent intent)) {
      throw new IllegalArgumentException("Stripe event does not contain a PaymentIntent");
    }
    Order order = orders.findByStripePaymentIntentId(intent.getId())
        .orElseThrow(() -> new IllegalArgumentException("Order not found for PaymentIntent"));

    if ("PAID".equals(order.getPaymentStatus())
        || "FAILED".equals(order.getPaymentStatus())
        || "CANCELLED".equals(order.getPaymentStatus())) {
      return;
    }

    if (failed && order.isStockReserved()) {
      for (OrderItem item : order.getItems()) {
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
      }
      order.setStockReserved(false);
      order.setPaymentStatus("FAILED");
      order.setStatus("CANCELLED");
    } else if (succeeded) {
      order.setStockReserved(false);
      order.setPaymentStatus("PAID");
      order.setStatus("PAID");
    }
    orders.save(order);
  }
}
