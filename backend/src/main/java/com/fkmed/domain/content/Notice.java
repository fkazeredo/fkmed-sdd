package com.fkmed.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An operator-managed Home notice (SPEC-0005 BR7), shown only while {@code active}.
 *
 * <p>Notices are operator-loaded reference content (seeded by migration in this phase); the
 * application never creates or mutates them at runtime yet.
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NoticeSeverity severity;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false)
  private boolean active;

  Notice(
      UUID id,
      String title,
      String body,
      NoticeSeverity severity,
      int displayOrder,
      boolean active) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("notice title is required");
    }
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("notice body is required");
    }
    if (severity == null) {
      throw new IllegalArgumentException("notice severity is required");
    }
    this.id = id;
    this.title = title;
    this.body = body;
    this.severity = severity;
    this.displayOrder = displayOrder;
    this.active = active;
  }

  /**
   * Creates a notice, validating its required fields.
   *
   * @throws IllegalArgumentException when title, body or severity is missing.
   */
  public static Notice create(
      String title, String body, NoticeSeverity severity, int displayOrder, boolean active) {
    return new Notice(UUID.randomUUID(), title, body, severity, displayOrder, active);
  }
}
