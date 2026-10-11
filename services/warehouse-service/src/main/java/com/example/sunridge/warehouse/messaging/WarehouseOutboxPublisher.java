package com.example.sunridge.warehouse.messaging;

import com.example.sunridge.warehouse.entity.WarehouseOutboxDeadLetter;
import com.example.sunridge.warehouse.entity.WarehouseOutboxEvent;
import com.example.sunridge.warehouse.repository.WarehouseOutboxDeadLetterRepository;
import com.example.sunridge.warehouse.repository.WarehouseOutboxEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WarehouseOutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(WarehouseOutboxPublisher.class);
  private final WarehouseOutboxEventRepository outboxEvents;
  private final WarehouseOutboxDeadLetterRepository deadLetters;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate transactions;
  private final int maxAttempts;

  public WarehouseOutboxPublisher(
      WarehouseOutboxEventRepository outboxEvents,
      WarehouseOutboxDeadLetterRepository deadLetters,
      KafkaTemplate<String, String> kafka,
      TransactionTemplate transactions,
      @Value("${outbox.max-attempts:5}") int maxAttempts) {
    this.outboxEvents = outboxEvents;
    this.deadLetters = deadLetters;
    this.kafka = kafka;
    this.transactions = transactions;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelay = 1000)
  public void publish() {
    List<WarehouseOutboxEvent> events =
        outboxEvents.findReadyToPublish(Instant.now(), PageRequest.of(0, 100));
    for (WarehouseOutboxEvent event : events) {
      try {
        kafka.send("warehouse-events", event.getAggregateId(), event.getPayload()).join();
        markPublished(event.getId());
      } catch (Exception exception) {
        log.warn("Could not publish warehouse outbox event {}", event.getId(), exception);
        recordFailure(event.getId(), rootMessage(exception));
      }
    }
  }

  private void markPublished(String eventId) {
    transactions.executeWithoutResult(
        ignored ->
            outboxEvents
                .findById(eventId)
                .ifPresent(event -> event.setPublishedAt(Instant.now())));
  }

  private void recordFailure(String eventId, String error) {
    transactions.executeWithoutResult(
        ignored ->
            outboxEvents
                .findById(eventId)
                .ifPresent(
                    event -> {
                      int attempts = event.getRetryCount() + 1;
                      event.setRetryCount(attempts);
                      event.setLastError(error);
                      if (attempts >= maxAttempts) {
                        deadLetters.save(WarehouseOutboxDeadLetter.from(event, error));
                        event.setDeadLetteredAt(Instant.now());
                      } else {
                        event.setNextAttemptAt(
                            Instant.now().plus(backoffSeconds(attempts), ChronoUnit.SECONDS));
                      }
                    }));
  }

  private long backoffSeconds(int attempts) {
    return Math.min(60, 1L << Math.min(attempts, 6));
  }

  private String rootMessage(Exception exception) {
    Throwable cause = exception;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String message = cause.getMessage();
    return message == null
        ? cause.getClass().getSimpleName()
        : message.substring(0, Math.min(2000, message.length()));
  }
}
