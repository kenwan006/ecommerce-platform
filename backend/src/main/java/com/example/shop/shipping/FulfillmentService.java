package com.example.shop.shipping;

import com.example.shop.entity.FulfillmentItem;
import com.example.shop.entity.FulfillmentOrder;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Shipment;
import com.example.shop.entity.Warehouse;
import com.example.shop.inventory.InventoryService;
import com.example.shop.repository.FulfillmentOrderRepository;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ShipmentRepository;
import com.example.shop.repository.WarehouseRepository;
import com.example.shop.shipping.model.DispatchShipmentRequest;
import com.example.shop.shipping.model.FulfillmentResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentService {
  private final OrderRepository orderRepository;
  private final WarehouseRepository warehouseRepository;
  private final FulfillmentOrderRepository fulfillmentRepository;
  private final ShipmentRepository shipmentRepository;
  private final InventoryService inventoryService;

  public FulfillmentService(
      OrderRepository orderRepository,
      WarehouseRepository warehouseRepository,
      FulfillmentOrderRepository fulfillmentRepository,
      ShipmentRepository shipmentRepository,
      InventoryService inventoryService) {
    this.orderRepository = orderRepository;
    this.warehouseRepository = warehouseRepository;
    this.fulfillmentRepository = fulfillmentRepository;
    this.shipmentRepository = shipmentRepository;
    this.inventoryService = inventoryService;
  }

  @Transactional
  public void createForPaidOrder(Long orderId) {
    if (fulfillmentRepository.findByOrderId(orderId).isPresent()) {
      return;
    }
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found for fulfillment"));
    Warehouse warehouse = warehouseRepository.findByCode("MAIN")
        .orElseThrow(() -> new IllegalStateException("Main warehouse is not configured"));
    FulfillmentOrder fulfillment = new FulfillmentOrder();
    fulfillment.setOrder(order);
    fulfillment.setWarehouse(warehouse);
    fulfillment.setStatus("READY_TO_PICK");
    for (OrderItem orderItem : order.getItems()) {
      FulfillmentItem item = new FulfillmentItem();
      item.setProduct(orderItem.getProduct());
      item.setQuantity(orderItem.getQuantity());
      fulfillment.addItem(item);
    }
    fulfillmentRepository.save(fulfillment);
  }

  @Transactional(readOnly = true)
  public List<FulfillmentResponse> readyToShip() {
    return fulfillmentRepository.findByStatusOrderByIdAsc("READY_TO_PICK").stream()
        .map(FulfillmentResponse::from)
        .toList();
  }

  @Transactional
  public void dispatch(Long fulfillmentId, DispatchShipmentRequest request) {
    FulfillmentOrder fulfillment = fulfillmentRepository.findById(fulfillmentId)
        .orElseThrow(() -> new IllegalArgumentException("Fulfillment order not found"));
    if (!"READY_TO_PICK".equals(fulfillment.getStatus())) {
      throw new IllegalStateException("Fulfillment order is not ready to ship");
    }
    inventoryService.ship(fulfillment.getOrder().getId());
    Shipment shipment = new Shipment();
    shipment.setFulfillmentOrder(fulfillment);
    shipment.setCarrier(request.carrier());
    shipment.setTrackingNumber(request.trackingNumber());
    shipmentRepository.save(shipment);
    for (FulfillmentItem item : fulfillment.getItems()) {
      item.setStatus("SHIPPED");
    }
    fulfillment.setStatus("SHIPPED");
    fulfillment.getOrder().setStatus("SHIPPED");
  }
}
