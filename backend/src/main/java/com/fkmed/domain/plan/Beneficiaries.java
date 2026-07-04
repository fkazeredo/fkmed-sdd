package com.fkmed.domain.plan;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public facade of the plan module (the module-boundary API consumed by {@code domain.identity} for
 * first access — DECISIONS-BASELINE §0001). Exposes only a {@link BeneficiaryMatch} view, never the
 * {@code Beneficiary} entity or its internal repository.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Beneficiaries {

  private final BeneficiaryRepository beneficiaries;

  /**
   * Matches the exact identity triple against the active beneficiary base (SPEC-0002 BR1). Any
   * divergence yields an empty result — the caller MUST answer a single generic refusal that never
   * reveals which field diverged.
   */
  public Optional<BeneficiaryMatch> matchForFirstAccess(
      String cpf, String cardNumber, LocalDate birthDate) {
    return beneficiaries
        .findByCpfAndCardNumberAndBirthDateAndActiveTrue(cpf, cardNumber, birthDate)
        .map(Beneficiaries::toMatch);
  }

  /** Resolves an active beneficiary by id (e.g. to bind the card claim at login). */
  public Optional<BeneficiaryMatch> findById(UUID beneficiaryId) {
    return beneficiaries.findByIdAndActiveTrue(beneficiaryId).map(Beneficiaries::toMatch);
  }

  private static BeneficiaryMatch toMatch(Beneficiary beneficiary) {
    return new BeneficiaryMatch(
        beneficiary.getId(),
        beneficiary.getCardNumber(),
        beneficiary.getBirthDate(),
        beneficiary.getRole());
  }
}
