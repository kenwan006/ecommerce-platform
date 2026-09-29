package com.example.shop.repository;

import com.example.shop.entity.Order;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<Order> findByUserIdAndPaymentStatusOrderByCreatedAtDesc(Long userId, String paymentStatus);

  Optional<Order> findByIdAndUserId(Long orderId, Long userId);

  Optional<Order> findByUserIdAndCheckoutId(Long userId, String checkoutId);

  Optional<Order> findByStripePaymentIntentId(String paymentIntentId);

  long countByUserIdAndCreatedAtAfter(Long userId, Instant createdAt);
}
