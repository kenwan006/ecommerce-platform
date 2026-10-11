package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.WarehouseOutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseOutboxEventRepository
    extends JpaRepository<WarehouseOutboxEvent, String> {
  @Query(
      """
      select event from WarehouseOutboxEvent event
      where event.publishedAt is null
        and event.deadLetteredAt is null
        and (event.nextAttemptAt is null or event.nextAttemptAt <= :now)
      order by event.occurredAt asc
      """)
  List<WarehouseOutboxEvent> findReadyToPublish(@Param("now") Instant now, Pageable pageable);
}
