package com.example.shop.service;

import com.example.shop.client.PaymentClient;
import com.example.shop.client.WarehouseClient;
import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.entity.Order;
import com.example.shop.fraud.FraudAssessmentService;
import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudDecisionResult;
import com.example.shop.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
  private final OrderRepository orders;
  private final OrderService ordersService;
  private final FraudAssessmentService fraudAssessmentService;
  private final WarehouseClient warehouseClient;
  private final PaymentClient paymentClient;

  public CheckoutService(
      OrderRepository orders,
      OrderService ordersService,
      FraudAssessmentService fraudAssessmentService,
      WarehouseClient warehouseClient,
      PaymentClient paymentClient) {
    this.orders = orders;
    this.ordersService = ordersService;
    this.fraudAssessmentService = fraudAssessmentService;
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
    warehouseClient.reserve(savedOrder);
    FraudDecisionResult fraudDecision = fraudAssessmentService.assess(savedOrder.getId());
    if (fraudDecision.decision() == FraudDecision.DECLINE) {
      throw new IllegalStateException("Checkout declined by fraud screening");
    }
    if (fraudDecision.decision() == FraudDecision.REVIEW) {
      throw new IllegalStateException("Checkout requires manual fraud review");
    }
    PaymentClient.Payment payment =
        paymentClient.create(
            savedOrder.getId(), savedOrder.getCheckoutId(), savedOrder.getTotal(), "usd");
    savedOrder.setStripePaymentIntentId(payment.providerPaymentId());
    orders.save(savedOrder);
    return new CheckoutResponse(
        savedOrder.getId(),
        payment.providerPaymentId(),
        payment.clientSecret(),
        payment.amount(),
        payment.currency(),
        savedOrder.getStatus());
  }
}
