package com.example.sunridge.notification.repository;

import com.example.sunridge.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
  boolean existsByProviderEventId(String providerEventId);
}
