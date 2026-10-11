package com.example.shop.repository;

import com.example.shop.entity.Return;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRepository extends JpaRepository<Return, Long> {
  Optional<Return> findByOrderId(Long orderId);
  Optional<Return> findByIdAndOrderUserId(Long id, Long userId);
}
