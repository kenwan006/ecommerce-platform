package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByCheckoutId(String checkoutId);

  Optional<Payment> findByProviderPaymentId(String providerPaymentId);

  Optional<Payment> findByCommerceOrderId(Long commerceOrderId);
}
