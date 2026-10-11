package com.example.sunridge.notification.repository;

import com.example.sunridge.notification.entity.DocumentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {
  Optional<DocumentRecord> findByCommerceOrderIdAndDocumentType(Long commerceOrderId, String documentType);
}
