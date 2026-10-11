package com.example.shop.client;

import com.example.shop.entity.Order;
import com.example.shop.entity.Product;
import com.example.shop.entity.Return;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WarehouseClient {
  private final RestClient restClient;

  public WarehouseClient(
      @Value("${services.warehouse.base-url:http://localhost:8082}") String baseUrl,
      @Value("${services.internal-api-key}") String internalApiKey) {
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
  }

  public void reserve(Order order) {
    for (var item : order.getItems()) {
      projectProduct(item.getProduct());
    }
    List<Item> items = order.getItems().stream()
        .map(item -> new Item(item.getProduct().getId(), item.getQuantity()))
        .toList();
    restClient.post().uri("/internal/reservations").contentType(MediaType.APPLICATION_JSON)
        .body(new Reservation(order.getId(), items)).retrieve().toBodilessEntity();
  }

  public void syncProducts(List<Product> products) {
    products.forEach(this::projectProduct);
  }

  public void confirm(Long orderId) {
    restClient.post().uri("/internal/reservations/{orderId}/confirm", orderId)
        .retrieve().toBodilessEntity();
  }

  public void release(Long orderId) {
    restClient.post().uri("/internal/reservations/{orderId}/release", orderId)
        .retrieve().toBodilessEntity();
  }

  public void inspectReturn(Return returnRequest, String disposition, String inspectionNote) {
    List<ReturnItem> items = returnRequest.getItems().stream()
        .map(item -> new ReturnItem(item.getProductId(), item.getQuantity()))
        .toList();
    restClient.post().uri("/internal/returns/inspect").contentType(MediaType.APPLICATION_JSON)
        .body(new ReturnInspection(
            returnRequest.getId(),
            returnRequest.getOrder().getId(),
            disposition,
            inspectionNote,
            items))
        .retrieve().toBodilessEntity();
  }

  private void projectProduct(Product product) {
    restClient
        .post()
        .uri("/internal/products")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            new ProductProjection(
                product.getId(), product.getSku(), product.getName(), product.getStock()))
        .retrieve()
        .toBodilessEntity();
  }

  private record ProductProjection(
      Long productId, String sku, String name, int initialOnHandQuantity) {}
  private record Reservation(Long orderId, List<Item> items) {}
  private record Item(Long productId, int quantity) {}
  private record ReturnInspection(
      Long returnId,
      Long orderId,
      String disposition,
      String inspectionNote,
      List<ReturnItem> items) {}
  private record ReturnItem(Long productId, int quantity) {}
}
