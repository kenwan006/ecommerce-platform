package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.InventoryReservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
  Optional<InventoryReservation> findByCommerceOrderId(Long commerceOrderId);

  List<InventoryReservation> findByStatusAndExpiresAtBefore(String status, Instant expiresAt);
}
