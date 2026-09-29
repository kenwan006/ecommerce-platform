package com.example.shop.service;

import com.example.shop.client.StripeClient;
import com.example.shop.client.PaymentClient;
import com.example.shop.client.WarehouseClient;
import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.entity.Order;
import com.example.shop.fraud.FraudAssessmentService;
import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudDecisionResult;
import com.example.shop.inventory.InventoryService;
import com.example.shop.repository.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
  private final OrderRepository orders;
  private final StripeClient stripeClient;
  private final OrderService ordersService;
  private final FraudAssessmentService fraudAssessmentService;
  private final InventoryService inventoryService;
  private final WarehouseClient warehouseClient;
  private final PaymentClient paymentClient;

  public CheckoutService(
      OrderRepository orders,
      StripeClient stripeClient,
      OrderService ordersService,
      FraudAssessmentService fraudAssessmentService,
      InventoryService inventoryService,
      WarehouseClient warehouseClient,
      PaymentClient paymentClient) {
    this.orders = orders;
    this.stripeClient = stripeClient;
    this.ordersService = ordersService;
    this.fraudAssessmentService = fraudAssessmentService;
    this.inventoryService = inventoryService;
    this.warehouseClient = warehouseClient;
    this.paymentClient = paymentClient;
  }

  public CheckoutResponse createCheckout(CheckoutRequest request) {
    Order savedOrder;
    try {
      // This transaction commits before Stripe is called. A timeout can therefore be retried
      // with the same local order ID and the same Stripe request parameters.
      savedOrder = ordersService.getOrCreateCheckout(request);
    } catch (DataIntegrityViolationException exception) {
      // Another request with this checkoutId won the unique-key race; reuse its order.
      savedOrder = ordersService.getCheckout(request.userId(), request.checkoutId());
    }
    if (warehouseClient.isEnabled()) {
      warehouseClient.reserve(savedOrder);
    } else {
      inventoryService.reserve(savedOrder.getId());
    }
    FraudDecisionResult fraudDecision = fraudAssessmentService.assess(savedOrder.getId());
    if (fraudDecision.decision() == FraudDecision.DECLINE) {
      throw new IllegalStateException("Checkout declined by fraud screening");
    }
    if (fraudDecision.decision() == FraudDecision.REVIEW) {
      throw new IllegalStateException("Checkout requires manual fraud review");
    }
    try {
      if (paymentClient.isEnabled()) {
        PaymentClient.Payment payment = paymentClient.create(
            savedOrder.getId(), savedOrder.getCheckoutId(), savedOrder.getTotal(), "usd");
        savedOrder.setStripePaymentIntentId(payment.providerPaymentId());
        orders.save(savedOrder);
        return new CheckoutResponse(savedOrder.getId(), payment.providerPaymentId(), payment.clientSecret(),
            payment.amount(), payment.currency(), savedOrder.getStatus());
      }
      PaymentIntent intent = stripeClient.createPaymentIntent(
          savedOrder.getTotal().movePointRight(2).longValueExact(),
          "usd",
          savedOrder.getId().toString(),
          request.userId().toString(),
          savedOrder.getCheckoutId());
      savedOrder.setStripePaymentIntentId(intent.getId());
      orders.save(savedOrder);
      return new CheckoutResponse(
          savedOrder.getId(), intent.getId(), intent.getClientSecret(), savedOrder.getTotal(), "usd", savedOrder.getStatus());
    } catch (StripeException exception) {
      throw new IllegalStateException("Could not start the Stripe payment: " + exception.getMessage());
    }
  }
}
