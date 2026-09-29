package com.example.sunridge.payment.controller;

import com.example.sunridge.payment.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {
  private final StripeWebhookService service;
  private final String secret;

  public StripeWebhookController(
      StripeWebhookService service, @Value("${stripe.webhook-secret}") String secret) {
    this.service = service;
    this.secret = secret;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> handle(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
    try {
      Event event = Webhook.constructEvent(payload, signature, secret);
      service.handle(event);
      return ResponseEntity.ok().build();
    } catch (SignatureVerificationException | IllegalArgumentException exception) {
      return ResponseEntity.badRequest().build();
    }
  }
}
