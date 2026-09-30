package com.example.sunridge.warehouse.carrier;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local-only helper that simulates a FedEx webhook after Warehouse has created a shipment. */
@Profile("local")
@RestController
@RequestMapping("/api/carriers/fedex/webhook/test")
public class LocalFedExWebhookTestController {
  private final CarrierWebhookService carrierWebhookService;

  public LocalFedExWebhookTestController(CarrierWebhookService carrierWebhookService) {
    this.carrierWebhookService = carrierWebhookService;
  }

  @PostMapping
  public ResponseEntity<Void> simulate(@Valid @RequestBody FedExTrackingEvent event) {
    carrierWebhookService.recordFedExStatus(event);
    return ResponseEntity.ok().build();
  }
}
