package com.example.sunridge.payment.service;

import com.example.sunridge.payment.entity.OutboxDeadLetter;
import com.example.sunridge.payment.entity.OutboxEvent;
import com.example.sunridge.payment.repository.OutboxDeadLetterRepository;
import com.example.sunridge.payment.repository.OutboxEventRepository;
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
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxEventRepository outbox;
  private final OutboxDeadLetterRepository deadLetters;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate transactions;
  private final int maxAttempts;

  public OutboxPublisher(
      OutboxEventRepository outbox,
      OutboxDeadLetterRepository deadLetters,
      KafkaTemplate<String, String> kafka,
      TransactionTemplate transactions,
      @Value("${outbox.max-attempts:5}") int maxAttempts) {
    this.outbox = outbox;
    this.deadLetters = deadLetters;
    this.kafka = kafka;
    this.transactions = transactions;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelay = 1000)
  public void publish() {
    List<OutboxEvent> events = outbox.findReadyToPublish(Instant.now(), PageRequest.of(0, 100));
    for (OutboxEvent event : events) {
      try {
        kafka.send("payment-events", event.getAggregateId(), event.getPayload()).join();
        markPublished(event.getId());
      } catch (Exception exception) {
        log.warn("Could not publish payment outbox event {}", event.getId(), exception);
        recordFailure(event.getId(), rootMessage(exception));
      }
    }
  }

  private void markPublished(String eventId) {
    transactions.executeWithoutResult(
        ignored ->
            outbox
                .findById(eventId)
                .ifPresent(event -> event.setPublishedAt(Instant.now())));
  }

  private void recordFailure(String eventId, String error) {
    transactions.executeWithoutResult(
        ignored ->
            outbox
                .findById(eventId)
                .ifPresent(
                    event -> {
                      int attempts = event.getRetryCount() + 1;
                      event.setRetryCount(attempts);
                      event.setLastError(error);
                      if (attempts >= maxAttempts) {
                        deadLetters.save(OutboxDeadLetter.from(event, error));
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
