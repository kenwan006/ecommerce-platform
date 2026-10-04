package com.example.shop.service;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.OrderResponse;
import com.example.shop.dto.RefundResponse;
import com.example.shop.client.PaymentClient;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.order.OrderEvent;
import com.example.shop.order.OrderStateMachine;
import com.example.shop.order.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OrderService {
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final PaymentClient paymentClient;
  private final OrderStateMachine orderStateMachine;

  public OrderService(
      UserRepository userRepository,
      ProductRepository productRepository,
      OrderRepository orderRepository,
      PaymentClient paymentClient,
      OrderStateMachine orderStateMachine) {
    this.userRepository = userRepository;
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.paymentClient = paymentClient;
    this.orderStateMachine = orderStateMachine;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> byUser(Long userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(OrderResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> pendingPaymentsByUser(Long userId) {
    return orderRepository.findByUserIdAndPaymentStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
        .map(OrderResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderResponse byIdForUser(Long orderId, Long userId) {
    return orderRepository.findByIdAndUserId(orderId, userId)
        .map(OrderResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
  }

  @Transactional(readOnly = true)
  public void requireOrderOwnership(Long orderId, Long userId) {
    if (orderRepository.findByIdAndUserId(orderId, userId).isEmpty()) {
      throw new ResourceNotFoundException("Order not found");
    }
  }

  /** Persists the local checkout operation before any call to an external payment provider. */
  @Transactional
  public Order getOrCreateCheckout(CheckoutRequest request) {
    Order existing = orderRepository.findByUserIdAndCheckoutId(request.userId(), request.checkoutId()).orElse(null);
    if (existing != null) {
      return existing;
    }

    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Order order = new Order();
    order.setUser(user);
    order.setCheckoutId(request.checkoutId());
    BigDecimal total = BigDecimal.ZERO;

    for (CheckoutRequest.Item requested : request.items()) {
      Product product = productRepository.findById(requested.productId())
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + requested.productId()));
      OrderItem item = new OrderItem();
      item.setProduct(product);
      item.setQuantity(requested.quantity());
      item.setUnitPrice(product.getPrice());
      order.addItem(item);
      total = total.add(product.getPrice().multiply(BigDecimal.valueOf(requested.quantity())));
    }

    order.setTotal(total);
    return orderRepository.save(order);
  }

  @Transactional(readOnly = true)
  public Order getCheckout(Long userId, String checkoutId) {
    return orderRepository.findByUserIdAndCheckoutId(userId, checkoutId)
        .orElseThrow(() -> new IllegalStateException("Checkout could not be recovered after a concurrent request"));
  }

  /** Refunds a paid order for its full amount after rechecking customer ownership. */
  @Transactional
  public RefundResponse refund(Long orderId, Long userId) {
    Order order = orderRepository.findByIdAndUserId(orderId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (order.getStatus() == OrderStatus.REFUNDED) {
      throw new IllegalStateException("This order has already been refunded.");
    }
    if (order.getStatus() != OrderStatus.PAID || !"PAID".equals(order.getPaymentStatus())) {
      throw new IllegalStateException("Only paid orders are eligible for a full refund.");
    }

    PaymentClient.Refund refund = paymentClient.refund(orderId);
    if (!"SUCCEEDED".equals(refund.status())) {
      throw new IllegalStateException("The payment provider did not complete the refund.");
    }
    order.setPaymentStatus("REFUNDED");
    orderStateMachine.transition(order, OrderEvent.REFUND_SUCCEEDED, refund.refundId());
    return new RefundResponse(orderId, refund.amount(), refund.currency(), refund.refundId(), refund.status());
  }
}
