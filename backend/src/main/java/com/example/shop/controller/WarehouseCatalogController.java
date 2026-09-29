package com.example.shop.controller;

import com.example.shop.client.WarehouseClient;
import com.example.shop.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseCatalogController {
  private final ProductRepository productRepository;
  private final WarehouseClient warehouseClient;

  public WarehouseCatalogController(
      ProductRepository productRepository, WarehouseClient warehouseClient) {
    this.productRepository = productRepository;
    this.warehouseClient = warehouseClient;
  }

  @PostMapping("/products/sync")
  public ResponseEntity<Void> syncProducts() {
    if (!warehouseClient.isEnabled()) return ResponseEntity.noContent().build();
    warehouseClient.syncProducts(productRepository.findAll());
    return ResponseEntity.noContent().build();
  }
}
