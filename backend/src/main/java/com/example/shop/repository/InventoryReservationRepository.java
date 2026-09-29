package com.example.shop.repository;

import com.example.shop.entity.InventoryReservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
  Optional<InventoryReservation> findByOrderId(Long orderId);
  List<InventoryReservation> findByStatusAndExpiresAtBefore(String status, Instant expiresAt);
}
