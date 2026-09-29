package com.example.sunridge.payment.service;

import com.example.sunridge.payment.repository.OutboxEventRepository;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
  private final OutboxEventRepository outbox;
  private final KafkaTemplate<String, String> kafka;

  public OutboxPublisher(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka) {
    this.outbox = outbox;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void publish() {
    for (var event : outbox.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
      kafka.send("payment-events", event.getAggregateId(), event.getPayload()).join();
      event.setPublishedAt(Instant.now());
    }
  }
}
