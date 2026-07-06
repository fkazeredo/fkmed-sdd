package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * An operator-managed FAQ question (SPEC-0014 BR5/BR6): a {@link FaqCategoryCodes} code, the
 * question (title), the answer (content) and its active flag. Read-only registry data seeded by
 * Flyway V25 — the application never creates or mutates it at runtime yet.
 */
@Entity
@Table(name = "faq_entry")
@Getter
public class FaqEntry {

  @Id private UUID id;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false, length = 200)
  private String question;

  @Column(nullable = false)
  private String answer;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false)
  private boolean active;

  /** JPA only. */
  protected FaqEntry() {}

  FaqEntry(
      UUID id, String category, String question, String answer, int displayOrder, boolean active) {
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("faq category is required");
    }
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("faq question is required");
    }
    if (answer == null || answer.isBlank()) {
      throw new IllegalArgumentException("faq answer is required");
    }
    this.id = id;
    this.category = category;
    this.question = question;
    this.answer = answer;
    this.displayOrder = displayOrder;
    this.active = active;
  }

  /**
   * Creates a FAQ entry, validating its required fields. Not called at runtime (entries are
   * operator-loaded via migration, like {@code domain.content.Banner}); exists for domain-logic
   * testability ({@link #matchesQuery}).
   */
  static FaqEntry create(
      String category, String question, String answer, int displayOrder, boolean active) {
    return new FaqEntry(UUID.randomUUID(), category, question, answer, displayOrder, active);
  }

  /**
   * Whether this entry matches {@code query} (SPEC-0014 BR5): filtered over both the question
   * (title) and the answer (content), case/accent-insensitive. A blank/{@code null} query matches
   * everything.
   */
  boolean matchesQuery(String query) {
    return NormalizedText.contains(question, query) || NormalizedText.contains(answer, query);
  }
}
