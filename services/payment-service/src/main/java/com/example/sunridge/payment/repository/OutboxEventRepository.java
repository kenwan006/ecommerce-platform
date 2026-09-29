package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
  List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
