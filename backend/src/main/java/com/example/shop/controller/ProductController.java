package com.example.shop.controller;

import com.example.shop.entity.Product;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.ProductRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping
  public ResponseEntity<?> getProduct(@RequestParam Long id) {
    Optional<Product> product = products.findById(id);
    if (product.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(product.get());
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
    }
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<?> resourceNotFound(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
}
