package com.example.sunridge.warehouse.controller;

import com.example.sunridge.warehouse.inventory.WarehouseInventoryService;
import com.example.sunridge.warehouse.model.FulfillmentResponse;
import com.example.sunridge.warehouse.model.InventoryAdjustmentRequest;
import com.example.sunridge.warehouse.model.InventoryMovementResponse;
import com.example.sunridge.warehouse.model.InventoryResponse;
import com.example.sunridge.warehouse.model.ProductProjectionRequest;
import com.example.sunridge.warehouse.model.ReservationRequest;
import com.example.sunridge.warehouse.model.ShipmentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WarehouseController {
  private final WarehouseInventoryService inventoryService;

  public WarehouseController(WarehouseInventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @PostMapping("/internal/products")
  public void upsertProduct(@Valid @RequestBody ProductProjectionRequest request) {
    inventoryService.upsertProduct(request);
  }

  @PostMapping("/internal/reservations")
  public void reserve(@Valid @RequestBody ReservationRequest request) {
    inventoryService.reserve(request);
  }

  @PostMapping("/internal/reservations/{orderId}/confirm")
  public void confirm(@PathVariable Long orderId) {
    inventoryService.confirm(orderId);
  }

  @PostMapping("/internal/reservations/{orderId}/release")
  public void release(@PathVariable Long orderId) {
    inventoryService.release(orderId);
  }

  @GetMapping("/api/warehouse/inventory")
  public List<InventoryResponse> inventory() {
    return inventoryService.levels();
  }

  @GetMapping("/api/warehouse/fulfillments")
  public List<FulfillmentResponse> fulfillments() {
    return inventoryService.readyToShip();
  }

  @GetMapping("/api/warehouse/inventory/movements")
  public List<InventoryMovementResponse> movements() {
    return inventoryService.movements();
  }

  @PostMapping("/api/warehouse/inventory/adjustments")
  public void adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
    inventoryService.adjust(request);
  }

  @PostMapping("/api/warehouse/fulfillments/{fulfillmentId}/ship")
  public void dispatch(
      @PathVariable Long fulfillmentId, @Valid @RequestBody ShipmentRequest request) {
    inventoryService.dispatch(fulfillmentId, request.carrier(), request.trackingNumber());
  }
}
