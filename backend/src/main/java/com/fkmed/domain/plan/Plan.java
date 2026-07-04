package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A health plan contract as registered at ANS (SPEC-0001 BR4).
 *
 * <p>Plans are operator-loaded reference mass (seeded by migration in this phase); the application
 * never creates or mutates them at runtime yet.
 */
@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "ans_registration", nullable = false)
  private String ansRegistration;

  @Column(nullable = false)
  private String coverage;

  @Column(nullable = false)
  private boolean copay;

  @Column(nullable = false)
  private boolean reimbursement;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false, columnDefinition = "text[]")
  private List<String> additives;

  Plan(
      UUID id,
      String name,
      String ansRegistration,
      String coverage,
      boolean copay,
      boolean reimbursement,
      List<String> additives) {
    this.id = id;
    this.name = name;
    this.ansRegistration = ansRegistration;
    this.coverage = coverage;
    this.copay = copay;
    this.reimbursement = reimbursement;
    this.additives = List.copyOf(additives);
  }

  /**
   * Creates a plan validating the ANS invariants.
   *
   * @throws IllegalArgumentException when name is blank or the ANS registration is not 6 digits.
   */
  public static Plan create(
      String name,
      String ansRegistration,
      String coverage,
      boolean copay,
      boolean reimbursement,
      List<String> additives) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("plan name is required");
    }
    if (ansRegistration == null || !ansRegistration.matches("\\d{6}")) {
      throw new IllegalArgumentException("ANS registration must have 6 numeric digits");
    }
    if (coverage == null || coverage.isBlank()) {
      throw new IllegalArgumentException("plan coverage is required");
    }
    return new Plan(
        UUID.randomUUID(), name, ansRegistration, coverage, copay, reimbursement, additives);
  }
}
