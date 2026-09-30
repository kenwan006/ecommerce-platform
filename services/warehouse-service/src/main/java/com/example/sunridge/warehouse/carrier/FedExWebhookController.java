package com.example.sunridge.warehouse.carrier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carriers/fedex/webhook")
public class FedExWebhookController {
  private final CarrierWebhookService carrierWebhookService;
  private final ObjectMapper objectMapper;
  private final String securityToken;

  public FedExWebhookController(
      CarrierWebhookService carrierWebhookService,
      ObjectMapper objectMapper,
      @Value("${fedex.webhook-security-token:}") String securityToken) {
    this.carrierWebhookService = carrierWebhookService;
    this.objectMapper = objectMapper;
    this.securityToken = securityToken;
  }

  @PostMapping
  public ResponseEntity<Void> receive(
      @RequestBody String payload, @RequestHeader("fdx-signature") String signature) {
    if (securityToken.isBlank()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    if (!signature.equals(expectedSignature(payload))) return ResponseEntity.badRequest().build();
    carrierWebhookService.recordFedExStatus(parse(payload));
    return ResponseEntity.ok().build();
  }

  private FedExTrackingEvent parse(String payload) {
    try {
      return objectMapper.readValue(payload, FedExTrackingEvent.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Invalid FedEx webhook payload", exception);
    }
  }

  private String expectedSignature(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(securityToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("Could not verify FedEx webhook signature", exception);
    }
  }
}
