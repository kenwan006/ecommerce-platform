package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.WarehouseOutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseOutboxEventRepository
    extends JpaRepository<WarehouseOutboxEvent, String> {
  List<WarehouseOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
