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

  /**
   * The plan's contracting/segmentation classification shown next to the plan name on the digital
   * card face (SPEC-0007 BR1) — distinct from {@link #coverage}, which is the ANS geographic-reach
   * seal (BR2). Kept a plain column (baseline §0019: no registry table yet for a single POC value —
   * DL-0010), not an enum (free-text operator label, not a state machine or a value fixed by law).
   */
  @Column(nullable = false)
  private String category;

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
      String category,
      boolean copay,
      boolean reimbursement,
      List<String> additives) {
    this.id = id;
    this.name = name;
    this.ansRegistration = ansRegistration;
    this.coverage = coverage;
    this.category = category;
    this.copay = copay;
    this.reimbursement = reimbursement;
    this.additives = List.copyOf(additives);
  }

  /**
   * Creates a plan validating the ANS invariants.
   *
   * @throws IllegalArgumentException when name is blank, the ANS registration is not 6 digits,
   *     coverage is blank or category is blank.
   */
  public static Plan create(
      String name,
      String ansRegistration,
      String coverage,
      String category,
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
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("plan category is required");
    }
    return new Plan(
        UUID.randomUUID(),
        name,
        ansRegistration,
        coverage,
        category,
        copay,
        reimbursement,
        additives);
  }
}
