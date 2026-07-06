package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/**
 * An operator-owned care unit where consultations and exams are scheduled (SPEC-0009 §Business
 * Context: scheduling covers the operator's own units only). Read-only registry data seeded by
 * Flyway V16. A {@link #virtual} unit has no physical address and backs the telemedicine agenda
 * (SPEC-0010 BR14, DL-0018): a booking against it is recorded as a {@code TELEMEDICINA} modality.
 */
@Entity
@Table(name = "care_unit")
@Getter
public class CareUnit {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  /** Whether this is the virtual Telemedicina unit backing the tele agenda (SPEC-0010, DL-0018). */
  @Column(nullable = false)
  private boolean virtual;

  private String cep;
  private String street;

  @Column(name = "address_number")
  private String addressNumber;

  private String complement;

  @Column(nullable = false)
  private String neighborhood;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String uf;

  @Column(nullable = false)
  private String phone;

  /** JPA only. */
  protected CareUnit() {}
}
