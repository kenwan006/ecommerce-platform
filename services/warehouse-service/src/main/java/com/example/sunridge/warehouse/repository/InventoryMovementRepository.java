package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.InventoryMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
  List<InventoryMovement> findTop20ByOrderByCreatedAtDesc();
}
