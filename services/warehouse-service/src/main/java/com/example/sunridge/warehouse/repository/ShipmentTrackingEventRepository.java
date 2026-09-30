package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.ShipmentTrackingEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingEventRepository
    extends JpaRepository<ShipmentTrackingEvent, Long> {
  Optional<ShipmentTrackingEvent> findByProviderEventId(String providerEventId);

  List<ShipmentTrackingEvent> findByShipmentTrackingNumberOrderByOccurredAtAsc(String trackingNumber);
}
