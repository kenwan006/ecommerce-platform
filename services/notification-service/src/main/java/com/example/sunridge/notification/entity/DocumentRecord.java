package com.example.sunridge.notification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/** Demo storage. Production documents belong in object storage, with metadata stored here. */
@Entity
@Table(name = "document_records")
public class DocumentRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long commerceOrderId;
  private String documentType;
  private String fileName;

  @Lob
  private byte[] content;

  private Instant createdAt = Instant.now();

  public static DocumentRecord create(
      Long orderId, String documentType, String fileName, byte[] content) {
    DocumentRecord document = new DocumentRecord();
    document.commerceOrderId = orderId;
    document.documentType = documentType;
    document.fileName = fileName;
    document.content = content;
    return document;
  }

  public String getFileName() {
    return fileName;
  }

  public byte[] getContent() {
    return content;
  }
}
