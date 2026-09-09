package com.example.shop.controller;

import com.example.shop.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

  private final StripeWebhookService webhookService;
  private final String webhookSecret;

  public StripeWebhookController(
      StripeWebhookService webhookService,
      @Value("${stripe.webhook-secret}") String webhookSecret) {
    this.webhookService = webhookService;
    this.webhookSecret = webhookSecret;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> handle(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String signature) {
    try {
      Event event = Webhook.constructEvent(payload, signature, webhookSecret);
      webhookService.handle(event);
      return ResponseEntity.ok().build();
    } catch (SignatureVerificationException | IllegalArgumentException exception) {
      logger.warn("Rejected Stripe webhook: {}", exception.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }
}
