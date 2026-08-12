package com.example.shop.controller;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
  private final CheckoutService checkoutService;

  public CheckoutController(CheckoutService checkoutService) {
    this.checkoutService = checkoutService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutResponse create(@Valid @RequestBody CheckoutRequest request, Authentication authentication) {
    Long authenticatedUserId = Long.valueOf(authentication.getName());
    if (!authenticatedUserId.equals(request.userId())) {
      throw new IllegalArgumentException("Checkout user does not match the authenticated user");
    }
    return checkoutService.createCheckout(request);
  }
}
