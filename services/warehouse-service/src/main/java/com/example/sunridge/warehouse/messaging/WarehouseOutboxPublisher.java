package com.example.sunridge.warehouse.messaging;

import com.example.sunridge.warehouse.repository.WarehouseOutboxEventRepository;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WarehouseOutboxPublisher {
  private final WarehouseOutboxEventRepository outboxEvents;
  private final KafkaTemplate<String, String> kafka;

  public WarehouseOutboxPublisher(
      WarehouseOutboxEventRepository outboxEvents, KafkaTemplate<String, String> kafka) {
    this.outboxEvents = outboxEvents;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void publish() {
    for (var event : outboxEvents.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
      kafka.send("warehouse-events", event.getAggregateId(), event.getPayload()).join();
      event.setPublishedAt(Instant.now());
    }
  }
}
