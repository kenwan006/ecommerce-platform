package com.example.sunridge.warehouse.inventory;

import com.example.sunridge.warehouse.entity.FulfillmentItem;
import com.example.sunridge.warehouse.entity.FulfillmentOrder;
import com.example.sunridge.warehouse.entity.InventoryLevel;
import com.example.sunridge.warehouse.entity.InventoryMovement;
import com.example.sunridge.warehouse.entity.InventoryReservation;
import com.example.sunridge.warehouse.entity.InventoryReservationItem;
import com.example.sunridge.warehouse.entity.Shipment;
import com.example.sunridge.warehouse.entity.Warehouse;
import com.example.sunridge.warehouse.entity.WarehouseProduct;
import com.example.sunridge.warehouse.model.FulfillmentResponse;
import com.example.sunridge.warehouse.model.InventoryAdjustmentRequest;
import com.example.sunridge.warehouse.model.InventoryMovementResponse;
import com.example.sunridge.warehouse.model.InventoryResponse;
import com.example.sunridge.warehouse.model.ProductProjectionRequest;
import com.example.sunridge.warehouse.model.ReservationRequest;
import com.example.sunridge.warehouse.repository.FulfillmentOrderRepository;
import com.example.sunridge.warehouse.repository.InventoryLevelRepository;
import com.example.sunridge.warehouse.repository.InventoryMovementRepository;
import com.example.sunridge.warehouse.repository.InventoryReservationRepository;
import com.example.sunridge.warehouse.repository.ShipmentRepository;
import com.example.sunridge.warehouse.repository.WarehouseProductRepository;
import com.example.sunridge.warehouse.repository.WarehouseRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseInventoryService {
  private final WarehouseRepository warehouseRepository;
  private final WarehouseProductRepository productRepository;
  private final InventoryLevelRepository levelRepository;
  private final InventoryReservationRepository reservationRepository;
  private final FulfillmentOrderRepository fulfillmentRepository;
  private final ShipmentRepository shipmentRepository;
  private final InventoryMovementRepository movementRepository;

  public WarehouseInventoryService(
      WarehouseRepository warehouseRepository,
      WarehouseProductRepository productRepository,
      InventoryLevelRepository levelRepository,
      InventoryReservationRepository reservationRepository,
      FulfillmentOrderRepository fulfillmentRepository,
      ShipmentRepository shipmentRepository,
      InventoryMovementRepository movementRepository) {
    this.warehouseRepository = warehouseRepository;
    this.productRepository = productRepository;
    this.levelRepository = levelRepository;
    this.reservationRepository = reservationRepository;
    this.fulfillmentRepository = fulfillmentRepository;
    this.shipmentRepository = shipmentRepository;
    this.movementRepository = movementRepository;
  }

  @Transactional
  public void upsertProduct(ProductProjectionRequest request) {
    WarehouseProduct product =
        productRepository.findById(request.productId()).orElseGet(WarehouseProduct::new);
    product.setProductId(request.productId());
    product.setSku(request.sku());
    product.setName(request.name());
    product.setActive(true);
    productRepository.save(product);
    levelRepository
        .findByWarehouseIdAndProductProductId(mainWarehouse().getId(), request.productId())
        .orElseGet(
            () -> {
              InventoryLevel level = new InventoryLevel();
              level.setWarehouse(mainWarehouse());
              level.setProduct(product);
              level.receive(request.initialOnHandQuantity());
              return levelRepository.save(level);
            });
  }

  @Transactional
  public void reserve(ReservationRequest request) {
    InventoryReservation existing =
        reservationRepository.findByCommerceOrderId(request.orderId()).orElse(null);
    if (existing != null
        && ("RESERVED".equals(existing.getStatus()) || "CONFIRMED".equals(existing.getStatus())))
      return;
    if (existing != null)
      throw new IllegalStateException("Reservation cannot be reused for this order");
    Warehouse warehouse = mainWarehouse();
    InventoryReservation reservation = new InventoryReservation();
    reservation.setCommerceOrderId(request.orderId());
    reservation.setStatus("RESERVED");
    reservation.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
    for (ReservationRequest.Item item : request.items()) {
      InventoryLevel level = level(warehouse, item.productId());
      level.reserve(item.quantity());
      InventoryReservationItem reservationItem = new InventoryReservationItem();
      reservationItem.setProductId(item.productId());
      reservationItem.setQuantity(item.quantity());
      reservation.addItem(reservationItem);
    }
    reservationRepository.save(reservation);
  }

  @Transactional
  public void confirm(Long orderId) {
    InventoryReservation reservation = reservation(orderId);
    if ("RESERVED".equals(reservation.getStatus())) {
      reservation.setStatus("CONFIRMED");
      createFulfillment(reservation);
    }
  }

  @Transactional
  public void release(Long orderId) {
    InventoryReservation reservation =
        reservationRepository.findByCommerceOrderId(orderId).orElse(null);
    if (reservation == null || !"RESERVED".equals(reservation.getStatus())) return;
    release(reservation);
    reservation.setStatus("RELEASED");
  }

  @Transactional
  public void adjust(InventoryAdjustmentRequest request) {
    InventoryLevel level = level(mainWarehouse(), request.productId());
    if ("IMPORT".equals(request.type())) level.receive(request.quantity());
    else if ("EXPORT".equals(request.type())) level.export(request.quantity());
    else throw new IllegalArgumentException("type must be IMPORT or EXPORT");
    record(
        mainWarehouse(), request.productId(), request.type(), request.quantity(), request.note());
  }

  @Transactional
  public void dispatch(Long fulfillmentId, String carrier, String trackingNumber) {
    FulfillmentOrder fulfillment =
        fulfillmentRepository
            .findById(fulfillmentId)
            .orElseThrow(() -> new IllegalArgumentException("Fulfillment not found"));
    if (!"READY_TO_PICK".equals(fulfillment.getStatus()))
      throw new IllegalStateException("Fulfillment is not ready to ship");
    InventoryReservation reservation = reservation(fulfillment.getCommerceOrderId());
    if (!"CONFIRMED".equals(reservation.getStatus()))
      throw new IllegalStateException("Inventory is not confirmed");
    Warehouse warehouse = mainWarehouse();
    reservation
        .getItems()
        .forEach(item -> level(warehouse, item.getProductId()).ship(item.getQuantity()));
    reservation
        .getItems()
        .forEach(
            item ->
                record(
                    warehouse,
                    item.getProductId(),
                    "SHIPMENT",
                    item.getQuantity(),
                    "Order #" + fulfillment.getCommerceOrderId()));
    reservation.setStatus("SHIPPED");
    fulfillment.setStatus("SHIPPED");
    Shipment shipment = new Shipment();
    shipment.setFulfillmentOrder(fulfillment);
    shipment.setCarrier(carrier);
    shipment.setTrackingNumber(trackingNumber);
    shipmentRepository.save(shipment);
  }

  @Transactional(readOnly = true)
  public List<InventoryResponse> levels() {
    return levelRepository.findAllByOrderByProduct_NameAsc().stream()
        .map(InventoryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FulfillmentResponse> readyToShip() {
    return fulfillmentRepository.findByStatusOrderByIdAsc("READY_TO_PICK").stream()
        .map(FulfillmentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryMovementResponse> movements() {
    return movementRepository.findTop20ByOrderByCreatedAtDesc().stream()
        .map(
            m ->
                new InventoryMovementResponse(
                    m.getId(),
                    productRepository
                        .findById(m.getProductId())
                        .map(WarehouseProduct::getName)
                        .orElse("Unknown"),
                    m.getType(),
                    m.getQuantity(),
                    m.getNote(),
                    m.getCreatedAt()))
        .toList();
  }

  @Scheduled(fixedDelay = 60_000)
  @Transactional
  public void expire() {
    reservationRepository
        .findByStatusAndExpiresAtBefore("RESERVED", Instant.now())
        .forEach(
            reservation -> {
              release(reservation);
              reservation.setStatus("EXPIRED");
            });
  }

  private Warehouse mainWarehouse() {
    return warehouseRepository
        .findByCode("MAIN")
        .orElseThrow(() -> new IllegalStateException("MAIN warehouse is missing"));
  }

  private InventoryReservation reservation(Long orderId) {
    return reservationRepository
        .findByCommerceOrderId(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
  }

  private InventoryLevel level(Warehouse warehouse, Long productId) {
    return levelRepository
        .findByWarehouseIdAndProductProductId(warehouse.getId(), productId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown warehouse product " + productId));
  }

  private void release(InventoryReservation reservation) {
    Warehouse warehouse = mainWarehouse();
    reservation
        .getItems()
        .forEach(item -> level(warehouse, item.getProductId()).release(item.getQuantity()));
  }

  private void createFulfillment(InventoryReservation reservation) {
    if (fulfillmentRepository.findByCommerceOrderId(reservation.getCommerceOrderId()).isPresent())
      return;
    FulfillmentOrder fulfillment = new FulfillmentOrder();
    fulfillment.setCommerceOrderId(reservation.getCommerceOrderId());
    fulfillment.setStatus("READY_TO_PICK");
    reservation
        .getItems()
        .forEach(
            item -> {
              WarehouseProduct product =
                  productRepository.findById(item.getProductId()).orElseThrow();
              FulfillmentItem fulfillmentItem = new FulfillmentItem();
              fulfillmentItem.setProductId(item.getProductId());
              fulfillmentItem.setProductName(product.getName());
              fulfillmentItem.setQuantity(item.getQuantity());
              fulfillmentItem.setStatus("PENDING_PICK");
              fulfillment.addItem(fulfillmentItem);
            });
    fulfillmentRepository.save(fulfillment);
  }

  private void record(Warehouse warehouse, Long productId, String type, int quantity, String note) {
    InventoryMovement movement = new InventoryMovement();
    movement.setWarehouseId(warehouse.getId());
    movement.setProductId(productId);
    movement.setType(type);
    movement.setQuantity(quantity);
    movement.setNote(note);
    movementRepository.save(movement);
  }
}
