package com.example.shop.service;

import com.example.shop.entity.Order;
import com.example.shop.client.WarehouseClient;
import com.example.shop.inventory.InventoryService;
import com.example.shop.shipping.FulfillmentService;
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
  private final InventoryService inventoryService;
  private final FulfillmentService fulfillmentService;
  private final WarehouseClient warehouseClient;

  public StripeWebhookService(
      OrderRepository orders,
      InventoryService inventoryService,
      FulfillmentService fulfillmentService,
      WarehouseClient warehouseClient) {
    this.orders = orders;
    this.inventoryService = inventoryService;
    this.fulfillmentService = fulfillmentService;
    this.warehouseClient = warehouseClient;
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

    if (failed) {
      if (warehouseClient.isEnabled()) warehouseClient.release(order.getId());
      else inventoryService.release(order.getId());
      order.setPaymentStatus("FAILED");
      order.setStatus("CANCELLED");
    } else if (succeeded) {
      if (warehouseClient.isEnabled()) warehouseClient.confirm(order.getId());
      else inventoryService.confirm(order.getId());
      order.setPaymentStatus("PAID");
      order.setStatus("PAID");
      if (!warehouseClient.isEnabled()) fulfillmentService.createForPaidOrder(order.getId());
    }
    orders.save(order);
  }
}
