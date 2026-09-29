package com.example.sunridge.warehouse.messaging;

import com.example.sunridge.warehouse.entity.ProcessedEvent;
import com.example.sunridge.warehouse.inventory.WarehouseInventoryService;
import com.example.sunridge.warehouse.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventsListener {
  private final ObjectMapper objectMapper;
  private final WarehouseInventoryService warehouseInventoryService;
  private final ProcessedEventRepository processedEventRepository;

  public PaymentEventsListener(
      ObjectMapper objectMapper,
      WarehouseInventoryService warehouseInventoryService,
      ProcessedEventRepository processedEventRepository) {
    this.objectMapper = objectMapper;
    this.warehouseInventoryService = warehouseInventoryService;
    this.processedEventRepository = processedEventRepository;
  }

  @KafkaListener(topics = "payment-events", groupId = "warehouse-service")
  @Transactional
  public void handle(String payload) {
    PaymentEvent event = parse(payload);
    if (processedEventRepository.existsById(event.eventId())) return;

    if ("PaymentSucceeded".equals(event.type())) {
      warehouseInventoryService.confirm(event.orderId());
    } else if ("PaymentFailed".equals(event.type())) {
      warehouseInventoryService.release(event.orderId());
    } else {
      throw new IllegalArgumentException("Unsupported payment event type: " + event.type());
    }
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
  }

  private PaymentEvent parse(String payload) {
    try {
      return objectMapper.readValue(payload, PaymentEvent.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Invalid payment event payload", exception);
    }
  }
}
