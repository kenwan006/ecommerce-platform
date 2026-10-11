package com.example.sunridge.notification.service;

import com.example.sunridge.notification.client.CommerceClient.OrderSummary;
import com.example.sunridge.notification.entity.DocumentRecord;
import com.example.sunridge.notification.repository.DocumentRecordRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {
  private final DocumentRecordRepository documents;

  public DocumentService(DocumentRecordRepository documents) {
    this.documents = documents;
  }

  @Transactional
  public DocumentRecord invoice(OrderSummary order) {
    return documents
        .findByCommerceOrderIdAndDocumentType(order.orderId(), "INVOICE")
        .orElseGet(
            () ->
                documents.save(
                    DocumentRecord.create(
                        order.orderId(),
                        "INVOICE",
                        "invoice-order-" + order.orderId() + ".pdf",
                        pdf(
                            "Sunridge invoice",
                            "Order: " + order.orderId(),
                            "Customer: " + order.customerName(),
                            "Total: " + order.currency() + " " + order.total()))));
  }

  @Transactional
  public DocumentRecord packingSlip(OrderSummary order, String carrier, String trackingNumber) {
    return documents
        .findByCommerceOrderIdAndDocumentType(order.orderId(), "PACKING_SLIP")
        .orElseGet(
            () ->
                documents.save(
                    DocumentRecord.create(
                        order.orderId(),
                        "PACKING_SLIP",
                        "packing-slip-order-" + order.orderId() + ".pdf",
                        pdf(
                            "Sunridge packing slip",
                            "Order: " + order.orderId(),
                            "Carrier: " + carrier,
                            "Tracking number: " + trackingNumber))));
  }

  private byte[] pdf(String title, String... lines) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, output);
      document.open();
      document.add(new Paragraph(title));
      for (String line : lines) {
        document.add(new Paragraph(line));
      }
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not generate PDF document", exception);
    }
  }
}
