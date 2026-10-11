package com.example.shop.messaging;

import com.example.shop.entity.ProcessedEvent;
import com.example.shop.repository.ProcessedEventRepository;
import com.example.shop.service.RefundService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Starts the Commerce-owned refund workflow after Warehouse records a return inspection. */
@Component
public class WarehouseEventsListener {
  private final ObjectMapper objectMapper;
  private final ProcessedEventRepository processedEvents;
  private final RefundService refundService;

  public WarehouseEventsListener(
      ObjectMapper objectMapper,
      ProcessedEventRepository processedEvents,
      RefundService refundService) {
    this.objectMapper = objectMapper;
    this.processedEvents = processedEvents;
    this.refundService = refundService;
  }

  @KafkaListener(topics = "warehouse-events", groupId = "commerce-service")
  @Transactional
  public void handle(String payload) {
    WarehouseEvent event = parse(payload);
    if (processedEvents.existsById(event.eventId())) return;
    if (!"ReturnInspected".equals(event.type())) {
      throw new IllegalArgumentException("Unsupported warehouse event type: " + event.type());
    }
    refundService.requestAfterInspection(event.returnId(), event.orderId());
    processedEvents.save(new ProcessedEvent(event.eventId()));
  }

  private WarehouseEvent parse(String payload) {
    try {
      return objectMapper.readValue(payload, WarehouseEvent.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Invalid warehouse event payload", exception);
    }
  }
}
