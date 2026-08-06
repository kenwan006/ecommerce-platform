package com.example.shop.service;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.OrderResponse;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OrderService {
  private final UserRepository users;
  private final ProductRepository products;
  private final OrderRepository orders;

  public OrderService(UserRepository users, ProductRepository products, OrderRepository orders) {
    this.users = users;
    this.products = products;
    this.orders = orders;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> byUser(Long userId) {
    return orders.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(OrderResponse::from)
        .toList();
  }

  /** Persists the local checkout operation before any call to an external payment provider. */
  @Transactional
  public Order getOrCreateCheckout(CheckoutRequest request) {
    Order existing = orders.findByUserIdAndCheckoutId(request.userId(), request.checkoutId()).orElse(null);
    if (existing != null) {
      return existing;
    }

    User user = users.findById(request.userId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Order order = new Order();
    order.setUser(user);
    order.setCheckoutId(request.checkoutId());
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
    return orders.saveAndFlush(order);
  }

  @Transactional(readOnly = true)
  public Order getCheckout(Long userId, String checkoutId) {
    return orders.findByUserIdAndCheckoutId(userId, checkoutId)
        .orElseThrow(() -> new IllegalStateException("Checkout could not be recovered after a concurrent request"));
  }
}
