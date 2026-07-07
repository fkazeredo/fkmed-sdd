package com.fkmed.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * The antifraud section content (SPEC-0014 BR3) — a single operator-managed row, destination of the
 * Home fraud banner (SPEC-0005 BR9). The 3 best-practice bullets and the validator link are static
 * frontend copy/structure (BR2 forbids hardcoded CONTACT info, not general guidance text); only the
 * title/message vary as operator content.
 */
@Entity
@Table(name = "support_antifraud")
@Getter
public class SupportAntifraud {

  @Id private UUID id;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String message;

  /** JPA only. */
  protected SupportAntifraud() {}
}
