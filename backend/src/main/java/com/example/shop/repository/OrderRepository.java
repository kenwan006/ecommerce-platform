package com.example.shop.repository;

import com.example.shop.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<Order> findByStripePaymentIntentId(String paymentIntentId);
}
