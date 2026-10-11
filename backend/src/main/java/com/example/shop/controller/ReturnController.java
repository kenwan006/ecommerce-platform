package com.example.shop.controller;

import com.example.shop.dto.ReturnRequest;
import com.example.shop.dto.ReturnResponse;
import com.example.shop.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReturnController {
  private final ReturnService returnService;

  public ReturnController(ReturnService returnService) {
    this.returnService = returnService;
  }

  @PostMapping("/orders/{orderId}/returns")
  public ReturnResponse request(
      @PathVariable Long orderId,
      @Valid @RequestBody ReturnRequest request,
      Authentication authentication) {
    return returnService.request(orderId, currentUserId(authentication), request);
  }

  @GetMapping("/returns/{returnId}")
  public ReturnResponse get(@PathVariable Long returnId, Authentication authentication) {
    return returnService.byIdForUser(returnId, currentUserId(authentication));
  }

  private Long currentUserId(Authentication authentication) {
    return Long.valueOf(authentication.getName());
  }
}
