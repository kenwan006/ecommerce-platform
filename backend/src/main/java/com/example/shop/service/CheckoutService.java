package com.example.shop.service;

import com.example.shop.client.StripeClient;
import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.entity.Order;
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

  public CheckoutService(
      OrderRepository orders,
      StripeClient stripeClient,
      OrderService ordersService) {
    this.orders = orders;
    this.stripeClient = stripeClient;
    this.ordersService = ordersService;
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
    try {
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
