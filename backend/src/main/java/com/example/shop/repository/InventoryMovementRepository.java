package com.example.shop.repository;

import com.example.shop.entity.InventoryMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
  List<InventoryMovement> findTop20ByOrderByCreatedAtDesc();
}
