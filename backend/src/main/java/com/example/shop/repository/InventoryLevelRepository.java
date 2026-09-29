package com.example.shop.repository;

import com.example.shop.entity.InventoryLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {
  Optional<InventoryLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
  List<InventoryLevel> findAllByOrderByProduct_NameAsc();
}
