package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * An operator-managed support channel card (SPEC-0014 BR1/BR2): a contact point of a fixed {@link
 * ChannelType}, its display label, its phone/URL value and optional operating hours. Read-only
 * registry data seeded by Flyway V25 — the application never creates or mutates it at runtime yet
 * (mirrors {@code domain.network.ServiceType}).
 */
@Entity
@Table(name = "support_channel")
@Getter
public class SupportChannel {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChannelType type;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private String value;

  private String hours;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** JPA only. */
  protected SupportChannel() {}
}
