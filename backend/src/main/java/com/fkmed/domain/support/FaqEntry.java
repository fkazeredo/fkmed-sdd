package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * A FAQ question (SPEC-0014 BR5/BR6) — operator-managed, seeded by migration in the POC. {@code
 * category} is a {@link FaqCategoryCodes} code; {@code active} lets content be retired without a
 * delete. Never written by application code (seed-only, read-only in this phase).
 */
@Entity
@Table(name = "faq_entry")
@Getter
public class FaqEntry {

  @Id private UUID id;

  @Column(nullable = false, length = 20)
  private String category;

  @Column(nullable = false, length = 200)
  private String question;

  @Column(nullable = false, columnDefinition = "text")
  private String answer;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false)
  private boolean active;

  /** JPA only. */
  protected FaqEntry() {}

  /** Test-fixture construction (package-private — production content arrives only via seed). */
  static FaqEntry of(
      UUID id, String category, String question, String answer, int displayOrder, boolean active) {
    FaqEntry entry = new FaqEntry();
    entry.id = id;
    entry.category = category;
    entry.question = question;
    entry.answer = answer;
    entry.displayOrder = displayOrder;
    entry.active = active;
    return entry;
  }
}
