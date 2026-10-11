package com.example.sunridge.warehouse.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sunridge.warehouse.entity.InventoryLevel;
import com.example.sunridge.warehouse.entity.Warehouse;
import com.example.sunridge.warehouse.entity.WarehouseOutboxEvent;
import com.example.sunridge.warehouse.model.ReturnInspectionRequest;
import com.example.sunridge.warehouse.repository.FulfillmentOrderRepository;
import com.example.sunridge.warehouse.repository.InventoryLevelRepository;
import com.example.sunridge.warehouse.repository.InventoryMovementRepository;
import com.example.sunridge.warehouse.repository.InventoryReservationRepository;
import com.example.sunridge.warehouse.repository.ReturnReceiptRepository;
import com.example.sunridge.warehouse.repository.ShipmentRepository;
import com.example.sunridge.warehouse.repository.WarehouseOutboxEventRepository;
import com.example.sunridge.warehouse.repository.WarehouseProductRepository;
import com.example.sunridge.warehouse.repository.WarehouseRepository;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WarehouseInventoryServiceTest {
  @Test
  void inspectionWritesReturnInspectedToWarehouseOutbox() {
    Warehouse warehouse = new Warehouse();
    setId(warehouse, 1L);
    InventoryLevel level = new InventoryLevel();
    List<WarehouseOutboxEvent> outboxEvents = new ArrayList<>();

    WarehouseInventoryService service =
        new WarehouseInventoryService(
            warehouseRepository(warehouse),
            unusedRepository(WarehouseProductRepository.class),
            levelRepository(level),
            unusedRepository(InventoryReservationRepository.class),
            unusedRepository(FulfillmentOrderRepository.class),
            unusedRepository(ShipmentRepository.class),
            inventoryMovementRepository(),
            returnReceiptRepository(),
            outboxRepository(outboxEvents));

    service.inspectReturn(
        new ReturnInspectionRequest(
            7L,
            42L,
            "SELLABLE",
            "Resellable item",
            List.of(new ReturnInspectionRequest.Item(11L, 2))));

    assertThat(level.getOnHandQuantity()).isEqualTo(2);
    assertThat(outboxEvents).hasSize(1);
    assertThat(outboxEvents.get(0).getAggregateId()).isEqualTo("7");
    assertThat(outboxEvents.get(0).getPayload())
        .contains("ReturnInspected", "\"returnId\":7", "\"orderId\":42");
  }

  private static WarehouseRepository warehouseRepository(Warehouse warehouse) {
    return proxy(
        WarehouseRepository.class,
        (method, arguments) ->
            method.getName().equals("findByCode") ? Optional.of(warehouse) : defaultValue(method.getReturnType()));
  }

  private static InventoryLevelRepository levelRepository(InventoryLevel level) {
    return proxy(
        InventoryLevelRepository.class,
        (method, arguments) ->
            method.getName().equals("findByWarehouseIdAndProductProductId")
                ? Optional.of(level)
                : defaultValue(method.getReturnType()));
  }

  private static InventoryMovementRepository inventoryMovementRepository() {
    return proxy(
        InventoryMovementRepository.class,
        (method, arguments) ->
            method.getName().equals("save") ? arguments[0] : defaultValue(method.getReturnType()));
  }

  private static ReturnReceiptRepository returnReceiptRepository() {
    return proxy(
        ReturnReceiptRepository.class,
        (method, arguments) -> {
          if (method.getName().equals("existsByCommerceReturnId")) return false;
          if (method.getName().equals("save")) return arguments[0];
          return defaultValue(method.getReturnType());
        });
  }

  private static WarehouseOutboxEventRepository outboxRepository(List<WarehouseOutboxEvent> events) {
    return proxy(
        WarehouseOutboxEventRepository.class,
        (method, arguments) -> {
          if (method.getName().equals("save")) {
            events.add((WarehouseOutboxEvent) arguments[0]);
            return arguments[0];
          }
          return defaultValue(method.getReturnType());
        });
  }

  private static <T> T unusedRepository(Class<T> type) {
    return proxy(type, (method, arguments) -> defaultValue(method.getReturnType()));
  }

  private static <T> T proxy(Class<T> type, Invocation invocation) {
    return type.cast(
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, arguments) -> invocation.invoke(method, arguments)));
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    return null;
  }

  private static void setId(Object target, Long id) {
    try {
      var field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  @FunctionalInterface
  private interface Invocation {
    Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
  }
}
