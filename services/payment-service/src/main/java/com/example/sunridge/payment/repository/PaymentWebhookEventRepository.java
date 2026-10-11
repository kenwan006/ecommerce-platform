package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
  boolean existsByProviderEventId(String providerEventId);
}
