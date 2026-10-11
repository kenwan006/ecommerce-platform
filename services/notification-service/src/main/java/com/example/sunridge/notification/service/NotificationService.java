package com.example.sunridge.notification.service;

import com.example.sunridge.notification.client.CommerceClient;
import com.example.sunridge.notification.client.CommerceClient.OrderSummary;
import com.example.sunridge.notification.entity.DocumentRecord;
import com.example.sunridge.notification.entity.NotificationDelivery;
import com.example.sunridge.notification.repository.NotificationDeliveryRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationDeliveryRepository deliveries;
  private final CommerceClient commerceClient;
  private final DocumentService documents;
  private final JavaMailSender mailSender;
  private final String from;

  public NotificationService(
      NotificationDeliveryRepository deliveries,
      CommerceClient commerceClient,
      DocumentService documents,
      JavaMailSender mailSender,
      @Value("${notifications.from}") String from) {
    this.deliveries = deliveries;
    this.commerceClient = commerceClient;
    this.documents = documents;
    this.mailSender = mailSender;
    this.from = from;
  }

  @Transactional
  public void paymentSucceeded(String eventId, Long orderId) {
    if (deliveries.existsByProviderEventId(eventId)) {
      return;
    }
    OrderSummary order = commerceClient.order(orderId);
    DocumentRecord invoice = documents.invoice(order);
    String subject = "Your Sunridge order #" + orderId + " is confirmed";
    send(order.customerEmail(), subject, "Payment received. Your invoice is attached.", invoice);
    deliveries.save(NotificationDelivery.sent(eventId, "PaymentSucceeded", orderId, order.customerEmail(), subject));
  }

  @Transactional
  public void shipmentCreated(String eventId, Long orderId, String carrier, String trackingNumber) {
    if (deliveries.existsByProviderEventId(eventId)) {
      return;
    }
    OrderSummary order = commerceClient.order(orderId);
    DocumentRecord packingSlip = documents.packingSlip(order, carrier, trackingNumber);
    String subject = "Your Sunridge order #" + orderId + " has shipped";
    String text =
        "Your package has shipped with " + carrier + ". Tracking number: " + trackingNumber;
    send(order.customerEmail(), subject, text, packingSlip);
    deliveries.save(NotificationDelivery.sent(eventId, "ShipmentCreated", orderId, order.customerEmail(), subject));
  }

  private void send(String recipient, String subject, String text, DocumentRecord document) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(text);
      helper.addAttachment(document.getFileName(), new ByteArrayResource(document.getContent()));
      mailSender.send(message);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not deliver notification email", exception);
    }
  }
}
