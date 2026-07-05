package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

/**
 * A person covered by a {@link Plan}: the contract titular or one of their dependents (SPEC-0001
 * BR5).
 *
 * <p>Invariants (SPEC-0001 §Validation Rules): card number has 9 numeric digits; CNS has 15 digits;
 * CPF has 11 digits with valid check digits; birth date is in the past; a dependent is always
 * linked to a titular and a titular is never linked to another beneficiary.
 */
@Entity
@Table(name = "beneficiary")
@Getter
public class Beneficiary {

  @Id private UUID id;

  /** JPA only. */
  protected Beneficiary() {}

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id", nullable = false)
  private Plan plan;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false)
  private String cpf;

  @Column(nullable = false)
  private String cns;

  @Column(name = "card_number", nullable = false, unique = true)
  private String cardNumber;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BeneficiaryRole role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "titular_id")
  private Beneficiary titular;

  @Column(nullable = false)
  private boolean active;

  private Beneficiary(
      Plan plan,
      String fullName,
      String cpf,
      String cns,
      String cardNumber,
      LocalDate birthDate,
      BeneficiaryRole role,
      Beneficiary titular) {
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("full name is required");
    }
    if (cardNumber == null || !cardNumber.matches("\\d{9}")) {
      throw new IllegalArgumentException("card number must have 9 numeric digits");
    }
    if (cns == null || !cns.matches("\\d{15}")) {
      throw new IllegalArgumentException("CNS must have 15 digits");
    }
    if (!isValidCpf(cpf)) {
      throw new IllegalArgumentException("CPF must have 11 digits with valid check digits");
    }
    if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("birth date must be in the past");
    }
    this.id = UUID.randomUUID();
    this.plan = plan;
    this.fullName = fullName;
    this.cpf = cpf;
    this.cns = cns;
    this.cardNumber = cardNumber;
    this.birthDate = birthDate;
    this.role = role;
    this.titular = titular;
    this.active = true;
  }

  /**
   * Creates the contract titular.
   *
   * @throws IllegalArgumentException when any identification invariant is violated.
   */
  public static Beneficiary titular(
      Plan plan, String fullName, String cpf, String cns, String cardNumber, LocalDate birthDate) {
    return new Beneficiary(
        plan, fullName, cpf, cns, cardNumber, birthDate, BeneficiaryRole.TITULAR, null);
  }

  /**
   * Creates a dependent linked to an existing titular.
   *
   * @throws IllegalArgumentException when the linked beneficiary is not a titular or any
   *     identification invariant is violated.
   */
  public static Beneficiary dependentOf(
      Beneficiary titular,
      String fullName,
      String cpf,
      String cns,
      String cardNumber,
      LocalDate birthDate) {
    if (titular == null || titular.getRole() != BeneficiaryRole.TITULAR) {
      throw new IllegalArgumentException("a dependent must be linked to a titular");
    }
    return new Beneficiary(
        titular.getPlan(),
        fullName,
        cpf,
        cns,
        cardNumber,
        birthDate,
        BeneficiaryRole.DEPENDENT,
        titular);
  }

  /**
   * Deactivates the beneficiary in the plan (operator registry action). An inactive beneficiary has
   * no digital card (SPEC-0007 BR10: "carteirinha indisponível") and is excluded from the
   * accessible-beneficiaries selector (SPEC-0003 BR5), though it stays resolvable within its
   * titular's family scope for the digital-card feature's 404-vs-409 distinction (see {@link
   * BeneficiaryAccess#cardDetailsFor}).
   */
  public void deactivate() {
    this.active = false;
  }

  /**
   * Validates a CPF: 11 numeric digits, not all repeated, with valid mod-11 check digits (SPEC-0001
   * §Validation Rules).
   */
  static boolean isValidCpf(String cpf) {
    if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
      return false;
    }
    return checkDigit(cpf, 9) == cpf.charAt(9) - '0' && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
  }

  private static int checkDigit(String cpf, int length) {
    int sum = 0;
    for (int i = 0; i < length; i++) {
      sum += (cpf.charAt(i) - '0') * (length + 1 - i);
    }
    int remainder = (sum * 10) % 11;
    return remainder == 10 ? 0 : remainder;
  }
}
