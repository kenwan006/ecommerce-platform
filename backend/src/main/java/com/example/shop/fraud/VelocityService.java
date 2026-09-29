package com.example.shop.fraud;

import com.example.shop.repository.OrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class VelocityService {
  private final OrderRepository orderRepository;

  public VelocityService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  /**
   * Demo implementation backed by MySQL. At high request volume, replace this with atomic,
   * expiring Redis counters.
   */
  public long recentCheckoutCount(Long userId) {
    return orderRepository.countByUserIdAndCreatedAtAfter(
        userId, Instant.now().minus(5, ChronoUnit.MINUTES));
  }
}
