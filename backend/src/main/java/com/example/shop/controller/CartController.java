package com.example.shop.controller;

import com.example.shop.dto.CartItemRequest;
import com.example.shop.dto.CartQuantityRequest;
import com.example.shop.dto.CartResponse;
import com.example.shop.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public CartResponse getCart(Authentication authentication) {
    return cartService.getCart(currentUserId(authentication));
  }

  @PostMapping("/items")
  public CartResponse addItem(
      @Valid @RequestBody CartItemRequest request, Authentication authentication) {
    return cartService.addItem(currentUserId(authentication), request);
  }

  @DeleteMapping("/items/{productId}")
  public CartResponse removeItem(@PathVariable Long productId, Authentication authentication) {
    return cartService.removeItem(currentUserId(authentication), productId);
  }

  @PatchMapping("/items/{productId}")
  public CartResponse updateQuantity(
      @PathVariable Long productId,
      @Valid @RequestBody CartQuantityRequest request,
      Authentication authentication) {
    return cartService.updateQuantity(currentUserId(authentication), productId, request);
  }

  @DeleteMapping
  public ResponseEntity<Void> clearCart(Authentication authentication) {
    cartService.clearCart(currentUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  private Long currentUserId(Authentication authentication) {
    return Long.valueOf(authentication.getName());
  }
}
