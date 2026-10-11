package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
  @Query(
      """
      select event from OutboxEvent event
      where event.publishedAt is null
        and event.deadLetteredAt is null
        and (event.nextAttemptAt is null or event.nextAttemptAt <= :now)
      order by event.occurredAt asc
      """)
  List<OutboxEvent> findReadyToPublish(@Param("now") Instant now, Pageable pageable);
}
