package com.example.sunridge.warehouse.carrier;

import com.example.sunridge.warehouse.entity.Shipment;
import com.example.sunridge.warehouse.entity.ShipmentTrackingEvent;
import com.example.sunridge.warehouse.repository.ShipmentRepository;
import com.example.sunridge.warehouse.repository.ShipmentTrackingEventRepository;
import com.example.sunridge.warehouse.model.ShipmentTrackingEventResponse;
import com.example.sunridge.warehouse.model.ShipmentTrackingResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarrierWebhookService {
  private static final Set<String> SUPPORTED_STATUSES =
      Set.of("LABEL_CREATED", "READY_FOR_PICKUP", "IN_TRANSIT", "DELIVERED", "EXCEPTION");

  private final ShipmentRepository shipmentRepository;
  private final ShipmentTrackingEventRepository trackingEventRepository;

  public CarrierWebhookService(
      ShipmentRepository shipmentRepository, ShipmentTrackingEventRepository trackingEventRepository) {
    this.shipmentRepository = shipmentRepository;
    this.trackingEventRepository = trackingEventRepository;
  }

  @Transactional
  public void recordFedExStatus(FedExTrackingEvent event) {
    if (!SUPPORTED_STATUSES.contains(event.status())) {
      throw new IllegalArgumentException("Unsupported FedEx shipment status: " + event.status());
    }
    String providerEventId =
        event.eventId() == null || event.eventId().isBlank()
            ? "local-fedex-" + UUID.randomUUID()
            : event.eventId();
    if (trackingEventRepository.findByProviderEventId(providerEventId).isPresent()) return;
    Shipment shipment =
        shipmentRepository
            .findByTrackingNumber(event.trackingNumber())
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found for tracking number"));
    shipment.setStatus(event.status());
    ShipmentTrackingEvent trackingEvent = new ShipmentTrackingEvent();
    trackingEvent.setShipment(shipment);
    trackingEvent.setProviderEventId(providerEventId);
    trackingEvent.setStatus(event.status());
    trackingEvent.setLocation(event.location());
    trackingEvent.setDescription(event.description());
    trackingEvent.setOccurredAt(event.occurredAt() == null ? Instant.now() : event.occurredAt());
    trackingEventRepository.save(trackingEvent);
  }

  @Transactional(readOnly = true)
  public java.util.List<ShipmentTrackingEventResponse> trackingEvents(String trackingNumber) {
    return trackingEventRepository.findByShipmentTrackingNumberOrderByOccurredAtAsc(trackingNumber).stream()
        .map(ShipmentTrackingEventResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ShipmentTrackingResponse trackingForOrder(Long orderId) {
    Shipment shipment =
        shipmentRepository
            .findByFulfillmentOrderCommerceOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Shipment has not been created for this order"));
    return new ShipmentTrackingResponse(
        orderId,
        shipment.getCarrier(),
        shipment.getTrackingNumber(),
        shipment.getStatus(),
        trackingEvents(shipment.getTrackingNumber()));
  }
}
