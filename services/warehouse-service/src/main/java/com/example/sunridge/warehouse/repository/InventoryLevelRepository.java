package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.InventoryLevel;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {
  Optional<InventoryLevel> findByWarehouseIdAndProductProductId(Long warehouseId, Long productId);

  /** Locks the stock row until the surrounding reservation transaction commits. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select level from InventoryLevel level
      where level.warehouse.id = :warehouseId
        and level.product.productId = :productId
      """)
  Optional<InventoryLevel> findForUpdate(
      @Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

  List<InventoryLevel> findAllByOrderByProduct_NameAsc();
}
