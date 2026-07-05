package com.fkmed.domain.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public facade of the plan module for the active-beneficiary context and family-scope
 * authorization (SPEC-0003 BR1-BR5). The plan module already owns the beneficiary/titular family
 * model, so the scope check lives here rather than in a new module (Rule Zero — DL-0004): a titular
 * may act for themselves and their dependents, a dependent only for themselves. Exposes only DTO
 * views, never the {@link Beneficiary} entity or CPF/CNS (BR8).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BeneficiaryAccess {

  private final BeneficiaryRepository beneficiaries;

  /**
   * The beneficiaries the caller (identified by their beneficiary card claim) may act for — titular
   * first, then dependents ordered by birth date (SPEC-0003 BR1/BR5). A dependent sees only
   * themselves. An absent/unknown card yields an empty list (no accessible beneficiaries).
   */
  public List<AccessibleBeneficiary> accessibleFor(String beneficiaryCard) {
    return caller(beneficiaryCard).map(this::accessibleEntities).orElseGet(List::of).stream()
        .map(BeneficiaryAccess::toAccessible)
        .toList();
  }

  /**
   * The card summary of {@code targetBeneficiaryId} when it falls within the caller's scope
   * (SPEC-0003 BR2/BR3). Anything outside the scope — including an absent card or an id that does
   * not exist — throws {@link BeneficiaryNotAccessibleException} (404) without revealing whether
   * the beneficiary exists.
   */
  public BeneficiarySummary summaryFor(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary callerBeneficiary =
        caller(beneficiaryCard).orElseThrow(BeneficiaryNotAccessibleException::new);
    return accessibleEntities(callerBeneficiary).stream()
        .filter(beneficiary -> beneficiary.getId().equals(targetBeneficiaryId))
        .findFirst()
        .map(BeneficiaryAccess::toSummary)
        .orElseThrow(BeneficiaryNotAccessibleException::new);
  }

  private Optional<Beneficiary> caller(String beneficiaryCard) {
    if (beneficiaryCard == null || beneficiaryCard.isBlank()) {
      return Optional.empty();
    }
    return beneficiaries.findByCardNumberAndActiveTrue(beneficiaryCard);
  }

  private List<Beneficiary> accessibleEntities(Beneficiary caller) {
    List<Beneficiary> accessible = new ArrayList<>();
    accessible.add(caller);
    if (caller.getRole() == BeneficiaryRole.TITULAR) {
      accessible.addAll(beneficiaries.findByTitularIdAndActiveTrueOrderByBirthDate(caller.getId()));
    }
    return accessible;
  }

  private static AccessibleBeneficiary toAccessible(Beneficiary beneficiary) {
    return new AccessibleBeneficiary(
        beneficiary.getId(), firstName(beneficiary.getFullName()), beneficiary.getRole());
  }

  private static BeneficiarySummary toSummary(Beneficiary beneficiary) {
    return new BeneficiarySummary(
        beneficiary.getId(),
        firstName(beneficiary.getFullName()),
        beneficiary.getFullName(),
        beneficiary.getRole(),
        beneficiary.getPlan().getName(),
        beneficiary.getCardNumber(),
        null);
  }

  private static String firstName(String fullName) {
    return fullName.strip().split("\\s+", 2)[0];
  }
}
