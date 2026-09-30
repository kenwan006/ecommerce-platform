package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
  Optional<Shipment> findByTrackingNumber(String trackingNumber);

  Optional<Shipment> findByFulfillmentOrderCommerceOrderId(Long commerceOrderId);
}
