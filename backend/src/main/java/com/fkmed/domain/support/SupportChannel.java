package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * An actionable contact card (SPEC-0014 BR1/BR2) — operator-managed, seeded by migration in the
 * POC. {@code sublabel} differentiates two rows sharing one {@link ChannelTypeCodes} type: the
 * Central 24h card has a "Capitais" row and a "Demais localidades" row. Never written by
 * application code (seed-only, read-only in this phase).
 */
@Entity
@Table(name = "support_channel")
@Getter
public class SupportChannel {

  @Id private UUID id;

  @Column(nullable = false, length = 20)
  private String type;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(length = 120)
  private String sublabel;

  @Column(nullable = false, length = 160)
  private String value;

  @Column(length = 160)
  private String hours;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** JPA only. */
  protected SupportChannel() {}
}
