package com.example.shop.repository;

import com.example.shop.entity.FraudAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long> {
  Optional<FraudAssessment> findByOrderId(Long orderId);
}
