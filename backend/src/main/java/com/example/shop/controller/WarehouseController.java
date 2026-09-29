package com.example.shop.controller;

import com.example.shop.inventory.InventoryService;
import com.example.shop.inventory.model.InventoryLevelResponse;
import com.example.shop.inventory.model.InventoryAdjustmentRequest;
import com.example.shop.inventory.model.InventoryMovementResponse;
import com.example.shop.shipping.FulfillmentService;
import com.example.shop.shipping.model.DispatchShipmentRequest;
import com.example.shop.shipping.model.FulfillmentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {
  private final InventoryService inventoryService;
  private final FulfillmentService fulfillmentService;

  public WarehouseController(InventoryService inventoryService, FulfillmentService fulfillmentService) {
    this.inventoryService = inventoryService;
    this.fulfillmentService = fulfillmentService;
  }

  @GetMapping("/inventory")
  public List<InventoryLevelResponse> inventory() {
    return inventoryService.levels();
  }

  @GetMapping("/inventory/movements")
  public List<InventoryMovementResponse> movements() {
    return inventoryService.recentMovements();
  }

  @PostMapping("/inventory/movements")
  public void adjustInventory(@Valid @RequestBody InventoryAdjustmentRequest request) {
    inventoryService.adjust(request);
  }

  @GetMapping("/fulfillments")
  public List<FulfillmentResponse> fulfillments() {
    return fulfillmentService.readyToShip();
  }

  @PostMapping("/fulfillments/{fulfillmentId}/ship")
  public void ship(
      @PathVariable Long fulfillmentId,
      @Valid @RequestBody DispatchShipmentRequest request) {
    fulfillmentService.dispatch(fulfillmentId, request);
  }
}
