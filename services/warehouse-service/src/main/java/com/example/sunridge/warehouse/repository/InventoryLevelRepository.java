package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.InventoryLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {
  Optional<InventoryLevel> findByWarehouseIdAndProductProductId(Long warehouseId, Long productId);

  List<InventoryLevel> findAllByOrderByProduct_NameAsc();
}
