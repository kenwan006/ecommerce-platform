package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.PaymentRefund;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
  Optional<PaymentRefund> findByCommerceReturnId(Long commerceReturnId);

  Optional<PaymentRefund> findByProviderRefundId(String providerRefundId);
}
