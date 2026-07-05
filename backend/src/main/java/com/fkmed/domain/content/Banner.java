package com.fkmed.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An operator-managed Home banner (SPEC-0005 BR6), shown only while {@code active} and inside its
 * optional validity window.
 *
 * <p>Banners are operator-loaded reference content (seeded by migration in this phase); the
 * application never creates or mutates them at runtime yet.
 */
@Entity
@Table(name = "banner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String text;

  private String image;

  @Column(name = "button_label", nullable = false)
  private String buttonLabel;

  @Column(name = "internal_destination", nullable = false)
  private String internalDestination;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "valid_from")
  private Instant validFrom;

  @Column(name = "valid_to")
  private Instant validTo;

  Banner(
      UUID id,
      String title,
      String text,
      String image,
      String buttonLabel,
      String internalDestination,
      int displayOrder,
      boolean active,
      Instant validFrom,
      Instant validTo) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("banner title is required");
    }
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("banner text is required");
    }
    if (buttonLabel == null || buttonLabel.isBlank()) {
      throw new IllegalArgumentException("banner button label is required");
    }
    if (internalDestination == null || internalDestination.isBlank()) {
      throw new IllegalArgumentException("banner internal destination is required");
    }
    if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
      throw new IllegalArgumentException("validFrom must not be after validTo");
    }
    this.id = id;
    this.title = title;
    this.text = text;
    this.image = image;
    this.buttonLabel = buttonLabel;
    this.internalDestination = internalDestination;
    this.displayOrder = displayOrder;
    this.active = active;
    this.validFrom = validFrom;
    this.validTo = validTo;
  }

  /**
   * Creates a banner, validating its required fields and the optional validity window.
   *
   * @throws IllegalArgumentException when a required field is blank or {@code validFrom} is after
   *     {@code validTo}.
   */
  public static Banner create(
      String title,
      String text,
      String image,
      String buttonLabel,
      String internalDestination,
      int displayOrder,
      boolean active,
      Instant validFrom,
      Instant validTo) {
    return new Banner(
        UUID.randomUUID(),
        title,
        text,
        image,
        buttonLabel,
        internalDestination,
        displayOrder,
        active,
        validFrom,
        validTo);
  }

  /**
   * Whether this banner should be shown at {@code instant} (SPEC-0005 BR6): {@code active} AND
   * inside its optional validity window. Both {@code validFrom} and {@code validTo}, when present,
   * are inclusive boundaries; a {@code null} bound means no limit on that side.
   */
  public boolean isVisibleAt(Instant instant) {
    return active
        && (validFrom == null || !instant.isBefore(validFrom))
        && (validTo == null || !instant.isAfter(validTo));
  }
}
