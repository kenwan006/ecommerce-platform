package com.example.shop.inventory;

import com.example.shop.entity.InventoryLevel;
import com.example.shop.entity.InventoryMovement;
import com.example.shop.entity.InventoryReservation;
import com.example.shop.entity.InventoryReservationItem;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Warehouse;
import com.example.shop.inventory.model.InventoryLevelResponse;
import com.example.shop.inventory.model.InventoryAdjustmentRequest;
import com.example.shop.inventory.model.InventoryMovementResponse;
import com.example.shop.inventory.model.InventoryMovementType;
import com.example.shop.repository.InventoryLevelRepository;
import com.example.shop.repository.InventoryMovementRepository;
import com.example.shop.repository.InventoryReservationRepository;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.WarehouseRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private static final String MAIN_WAREHOUSE = "MAIN";
  private static final String RESERVED = "RESERVED";
  private static final String CONFIRMED = "CONFIRMED";
  private static final String RELEASED = "RELEASED";
  private static final String EXPIRED = "EXPIRED";
  private static final String SHIPPED = "SHIPPED";

  private final OrderRepository orderRepository;
  private final WarehouseRepository warehouseRepository;
  private final InventoryLevelRepository levelRepository;
  private final InventoryMovementRepository movementRepository;
  private final InventoryReservationRepository reservationRepository;

  public InventoryService(
      OrderRepository orderRepository,
      WarehouseRepository warehouseRepository,
      InventoryLevelRepository levelRepository,
      InventoryMovementRepository movementRepository,
      InventoryReservationRepository reservationRepository) {
    this.orderRepository = orderRepository;
    this.warehouseRepository = warehouseRepository;
    this.levelRepository = levelRepository;
    this.movementRepository = movementRepository;
    this.reservationRepository = reservationRepository;
  }

  @Transactional
  public void reserve(Long orderId) {
    InventoryReservation existing = reservationRepository.findByOrderId(orderId).orElse(null);
    if (existing != null && (RESERVED.equals(existing.getStatus()) || CONFIRMED.equals(existing.getStatus()))) {
      return;
    }
    if (existing != null) {
      throw new IllegalStateException("Inventory reservation can no longer be used for this order");
    }
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found for inventory reservation"));
    Warehouse warehouse = mainWarehouse();
    InventoryReservation reservation = new InventoryReservation();
    reservation.setOrder(order);
    reservation.setWarehouse(warehouse);
    reservation.setStatus(RESERVED);
    reservation.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));

    for (OrderItem orderItem : order.getItems()) {
      InventoryLevel level = levelRepository.findByWarehouseIdAndProductId(
          warehouse.getId(), orderItem.getProduct().getId())
          .orElseThrow(() -> new IllegalStateException("No inventory level exists for "
              + orderItem.getProduct().getName()));
      level.reserve(orderItem.getQuantity());

      InventoryReservationItem item = new InventoryReservationItem();
      item.setProduct(orderItem.getProduct());
      item.setQuantity(orderItem.getQuantity());
      reservation.addItem(item);
    }
    reservationRepository.save(reservation);
    order.setStockReserved(true);
  }

  @Transactional
  public void confirm(Long orderId) {
    InventoryReservation reservation = reservation(orderId);
    if (RESERVED.equals(reservation.getStatus())) {
      reservation.setStatus(CONFIRMED);
    }
  }

  @Transactional
  public void release(Long orderId) {
    InventoryReservation reservation = reservationRepository.findByOrderId(orderId).orElse(null);
    if (reservation == null || !RESERVED.equals(reservation.getStatus())) {
      return;
    }
    releaseQuantities(reservation);
    reservation.setStatus(RELEASED);
    reservation.getOrder().setStockReserved(false);
  }

  @Transactional
  public void ship(Long orderId) {
    InventoryReservation reservation = reservation(orderId);
    if (SHIPPED.equals(reservation.getStatus())) {
      return;
    }
    if (!CONFIRMED.equals(reservation.getStatus())) {
      throw new IllegalStateException("Inventory reservation is not confirmed for shipment");
    }
    for (InventoryReservationItem item : reservation.getItems()) {
      InventoryLevel level = levelRepository.findByWarehouseIdAndProductId(
          mainWarehouse().getId(), item.getProduct().getId())
          .orElseThrow(() -> new IllegalStateException("Inventory level not found for shipment"));
      level.ship(item.getQuantity());
      recordMovement(mainWarehouse(), item.getProduct(), InventoryMovementType.SHIPMENT,
          item.getQuantity(), "Order #" + orderId);
    }
    reservation.setStatus(SHIPPED);
    reservation.getOrder().setStockReserved(false);
  }

  @Transactional(readOnly = true)
  public List<InventoryLevelResponse> levels() {
    return levelRepository.findAllByOrderByProduct_NameAsc().stream()
        .map(InventoryLevelResponse::from)
        .toList();
  }

  @Transactional
  public void adjust(InventoryAdjustmentRequest request) {
    if (request.type() != InventoryMovementType.IMPORT && request.type() != InventoryMovementType.EXPORT) {
      throw new IllegalArgumentException("Only IMPORT and EXPORT are supported for manual inventory updates");
    }
    Warehouse warehouse = mainWarehouse();
    InventoryLevel level = levelRepository.findByWarehouseIdAndProductId(warehouse.getId(), request.productId())
        .orElseThrow(() -> new IllegalArgumentException("Inventory level not found for product " + request.productId()));
    if (request.type() == InventoryMovementType.IMPORT) {
      level.receive(request.quantity());
    } else {
      level.export(request.quantity());
    }
    recordMovement(warehouse, level.getProduct(), request.type(), request.quantity(), request.note());
  }

  @Transactional(readOnly = true)
  public List<InventoryMovementResponse> recentMovements() {
    return movementRepository.findTop20ByOrderByCreatedAtDesc().stream()
        .map(InventoryMovementResponse::from)
        .toList();
  }

  @Scheduled(fixedDelay = 60_000)
  @Transactional
  public void releaseExpiredReservations() {
    for (InventoryReservation reservation : reservationRepository
        .findByStatusAndExpiresAtBefore(RESERVED, Instant.now())) {
      releaseQuantities(reservation);
      reservation.setStatus(EXPIRED);
      reservation.getOrder().setStockReserved(false);
    }
  }

  private InventoryReservation reservation(Long orderId) {
    return reservationRepository.findByOrderId(orderId)
        .orElseThrow(() -> new IllegalStateException("Inventory reservation not found"));
  }

  private Warehouse mainWarehouse() {
    return warehouseRepository.findByCode(MAIN_WAREHOUSE)
        .orElseThrow(() -> new IllegalStateException("Main warehouse is not configured"));
  }

  private void releaseQuantities(InventoryReservation reservation) {
    Long warehouseId = mainWarehouse().getId();
    for (InventoryReservationItem item : reservation.getItems()) {
      InventoryLevel level = levelRepository.findByWarehouseIdAndProductId(warehouseId, item.getProduct().getId())
          .orElseThrow(() -> new IllegalStateException("Inventory level not found for release"));
      level.release(item.getQuantity());
    }
  }

  private void recordMovement(
      Warehouse warehouse,
      com.example.shop.entity.Product product,
      InventoryMovementType type,
      int quantity,
      String note) {
    InventoryMovement movement = new InventoryMovement();
    movement.setWarehouse(warehouse);
    movement.setProduct(product);
    movement.setType(type.name());
    movement.setQuantity(quantity);
    movement.setNote(note);
    movementRepository.save(movement);
  }
}
