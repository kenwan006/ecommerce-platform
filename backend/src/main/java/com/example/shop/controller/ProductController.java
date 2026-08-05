package com.example.shop.controller;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductRepository products;

  public ProductController(ProductRepository products) {
    this.products = products;
  }

  @GetMapping
  public List<Product> all() {
    return products.findAll();
  }

  @GetMapping("/{id}")
  public Product one(@PathVariable Long id) {
    return products
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
  }
}
