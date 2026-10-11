package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.ReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnReceiptRepository extends JpaRepository<ReturnReceipt, Long> {
  boolean existsByCommerceReturnId(Long commerceReturnId);
}
