package com.example.sunridge.payment.controller;

import com.example.sunridge.payment.model.CreatePaymentRequest;
import com.example.sunridge.payment.model.CreatePaymentResponse;
import com.example.sunridge.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
public class PaymentController {
  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  public CreatePaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
    return paymentService.create(request);
  }
}
