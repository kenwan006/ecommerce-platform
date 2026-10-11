package com.example.shop.controller;

import com.example.shop.dto.ReturnResponse;
import com.example.shop.service.ReturnService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Warehouse callback boundary. Production must protect this with service-to-service authentication. */
@RestController
@RequestMapping("/internal/returns")
public class InternalReturnController {
  private final ReturnService returnService;

  public InternalReturnController(ReturnService returnService) {
    this.returnService = returnService;
  }

  @PostMapping("/{returnId}/inspect")
  public ReturnResponse inspect(
      @PathVariable Long returnId,
      @RequestParam String disposition,
      @RequestParam(required = false) String note) {
    return returnService.inspectAndRefund(returnId, disposition, note);
  }
}
