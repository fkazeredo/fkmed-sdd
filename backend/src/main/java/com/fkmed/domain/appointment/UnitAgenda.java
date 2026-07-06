package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * A (unit, scope) offering: that a care unit serves a given specialty (consultation) or exam
 * (SPEC-0009 BR3/BR4). Groups the {@link ScheduleSlot}s of that scope. The scope code is a
 * specialty code from the network registry when {@code scopeType} is {@code CONSULTATION}, or an
 * {@link ExamType} code when it is {@code EXAM}; there is no FK because it spans two registries.
 */
@Entity
@Table(name = "unit_agenda")
@Getter
public class UnitAgenda {

  @Id private UUID id;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false)
  private AppointmentType scopeType;

  @Column(name = "scope_code", nullable = false)
  private String scopeCode;

  /** JPA only. */
  protected UnitAgenda() {}
}
