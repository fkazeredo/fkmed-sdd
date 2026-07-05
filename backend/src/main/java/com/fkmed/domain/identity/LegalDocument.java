package com.fkmed.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A published version of a legal document — Terms of Use or Privacy Notice (SPEC-0006 BR8). The
 * current version per type is the latest by {@link #publishedAt}; superseding a version inserts a
 * new row (history preserved), never mutates one. {@code type} is a public API/catalogue code
 * ({@link LegalDocumentTypes#TERMS}/{@link LegalDocumentTypes#PRIVACY}).
 */
@Entity
@Table(name = "legal_document")
@Getter
public class LegalDocument {

  @Id private UUID id;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String version;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  @Column(nullable = false)
  private String body;

  /** JPA only. */
  protected LegalDocument() {}

  private LegalDocument(UUID id, String type, String version, Instant publishedAt, String body) {
    this.id = id;
    this.type = type;
    this.version = version;
    this.publishedAt = publishedAt;
    this.body = body;
  }

  /** Publishes a document version (used by seeds/tests; the operator flow adds a new row). */
  public static LegalDocument publish(
      String type, String version, Instant publishedAt, String body) {
    return new LegalDocument(UUID.randomUUID(), type, version, publishedAt, body);
  }
}
