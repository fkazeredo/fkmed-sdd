package com.fkmed.domain.guides;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * One requested procedure/exam of a {@link Guide} (SPEC-0012 BR5): the TUSS code, its description,
 * the requested quantity and its own authorization status (BR6) — the guide's overall status
 * derives from the aggregate of its items' statuses ({@link Guide#deriveStatusFromItems()}).
 */
@Entity
@Table(name = "guide_item")
@Getter
public class GuideItem {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "guide_id", nullable = false)
  private Guide guide;

  @Column(name = "tuss_code", nullable = false)
  private String tussCode;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GuideItemStatus status;

  /** JPA only. */
  protected GuideItem() {}

  private GuideItem(Guide guide, String tussCode, String description, int quantity) {
    this.id = UUID.randomUUID();
    this.guide = guide;
    this.tussCode = tussCode;
    this.description = description;
    this.quantity = quantity;
    this.status = GuideItemStatus.EM_ANALISE;
  }

  /** Creates a new item bound to {@code guide}, starting {@link GuideItemStatus#EM_ANALISE}. */
  static GuideItem of(Guide guide, GuideItemInput input) {
    return new GuideItem(guide, input.tussCode(), input.description(), input.quantity());
  }

  /**
   * Applies an operator decision to this item (BR6). Guarded by {@link Guide}'s own transitions.
   */
  void applyStatus(GuideItemStatus status) {
    this.status = status;
  }
}
