package com.example.shop.service;

import com.example.shop.client.StripeClient;
import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {
  private final UserRepository users;
  private final ProductRepository products;
  private final OrderRepository orders;
  private final StripeClient stripeClient;

  public CheckoutService(
      UserRepository users,
      ProductRepository products,
      OrderRepository orders,
      StripeClient stripeClient) {
    this.users = users;
    this.products = products;
    this.orders = orders;
    this.stripeClient = stripeClient;
  }

  @Transactional
  public CheckoutResponse createCheckout(CheckoutRequest request) {
    User user = users.findById(request.userId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Order order = new Order();
    order.setUser(user);
    BigDecimal total = BigDecimal.ZERO;

    for (CheckoutRequest.Item requested : request.items()) {
      Product product = products.findById(requested.productId())
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + requested.productId()));
      if (product.getStock() < requested.quantity()) {
        throw new IllegalStateException(product.getName() + " is out of stock");
      }
      product.setStock(product.getStock() - requested.quantity());

      OrderItem item = new OrderItem();
      item.setProduct(product);
      item.setQuantity(requested.quantity());
      item.setUnitPrice(product.getPrice());
      order.addItem(item);
      total = total.add(product.getPrice().multiply(BigDecimal.valueOf(requested.quantity())));
    }

    order.setTotal(total);
    order.setStockReserved(true);
    Order savedOrder = orders.saveAndFlush(order);
    try {
      PaymentIntent intent = stripeClient.createPaymentIntent(
          total.movePointRight(2).longValueExact(),
          "usd",
          savedOrder.getId().toString(),
          user.getId().toString(),
          request.checkoutId());
      savedOrder.setStripePaymentIntentId(intent.getId());
      orders.save(savedOrder);
      return new CheckoutResponse(
          savedOrder.getId(), intent.getId(), intent.getClientSecret(), total, "usd", savedOrder.getStatus());
    } catch (StripeException exception) {
      throw new IllegalStateException("Could not start the Stripe payment: " + exception.getMessage());
    }
  }
}
