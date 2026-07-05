package com.fkmed.domain.network;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A provider service type — registry data, not an enum (SPEC-0008 BR5, baseline §0019): {@code
 * code} is the stable identifier the API filters by, {@code name} the editable pt-BR label, {@code
 * sortOrder} the fixed display order (BR5 lists the 8 types in a specific order) and {@code
 * hasSpecialtyStep} whether the funnel's specialty step applies to this type — only "Consultórios –
 * Clínicas – Terapias" carries it (BR5); every other type skips straight to results. Seeded by
 * Flyway V15; read-only at runtime in this phase.
 */
@Entity
@Table(name = "service_type")
@Getter
public class ServiceType {

  @Id private String code;

  @Column(nullable = false)
  private String name;

  @Column(name = "has_specialty_step", nullable = false)
  private boolean hasSpecialtyStep;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /** JPA only. */
  protected ServiceType() {}

  /** Whether this service type carries the funnel's specialty step (BR5). */
  public boolean specialtyStepApplies() {
    return hasSpecialtyStep;
  }

  /**
   * BR5 enforced server-side, not just by the frontend funnel: a specialty filter only ever applies
   * within the specialty-step service type. For every other type, any client-supplied specialty is
   * cleared rather than silently applied to a type that carries no specialties. Extracted as a pure
   * static method for unit testing (mirrors {@code domain.notification.NotificationEventType}'s
   * {@code emailEnabled} precedent).
   */
  static String clearSpecialtyOutsideItsStep(boolean specialtyStepApplies, String specialtyCode) {
    return specialtyStepApplies ? specialtyCode : null;
  }
}
