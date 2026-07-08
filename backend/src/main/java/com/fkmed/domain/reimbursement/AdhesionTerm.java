package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

/**
 * A published version of the reimbursement adhesion term (SPEC-0015 BR3) — including the 5-year
 * original-document-retention declaration. Seed-only, read-only in this phase (no in-product
 * authoring UI).
 */
@Entity
@Table(name = "reimbursement_adhesion_term")
@Getter
public class AdhesionTerm {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 10)
  private String version;

  @Column(name = "published_at", nullable = false)
  private LocalDate publishedAt;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  /** JPA only. */
  protected AdhesionTerm() {}
}
