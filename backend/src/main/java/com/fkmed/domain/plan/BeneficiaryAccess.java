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
 * views, never the {@link Beneficiary} entity or CPF/CNS (BR8) — except {@link #cardDetailsFor},
 * the deliberate SPEC-0007 BR8 exception for the digital-card feature.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BeneficiaryAccess {

  private final BeneficiaryRepository beneficiaries;
  private final BeneficiaryPhotoRepository photos;

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
    Beneficiary target = requireInScope(beneficiaryCard, targetBeneficiaryId);
    return toSummary(target, avatarUrlFor(target.getId()));
  }

  /**
   * Resolves {@code targetBeneficiaryId} to its entity when it falls within the caller's active
   * family scope (SPEC-0003 BR2/BR3), for the profile/contacts/photo flows (SPEC-0006). Anything
   * outside the scope — absent card, unknown id, or a beneficiary the caller may not act for (a
   * dependent editing the titular, PEDRO→MARIA) — throws {@link BeneficiaryNotAccessibleException}
   * (404) without revealing whether the beneficiary exists. Package-private: the entity never
   * leaves the module.
   */
  Beneficiary requireInScope(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary callerBeneficiary =
        caller(beneficiaryCard).orElseThrow(BeneficiaryNotAccessibleException::new);
    return accessibleEntities(callerBeneficiary).stream()
        .filter(beneficiary -> beneficiary.getId().equals(targetBeneficiaryId))
        .findFirst()
        .orElseThrow(BeneficiaryNotAccessibleException::new);
  }

  /**
   * Scope-checks a target beneficiary for a write another module performs on their behalf and
   * returns the selector DTO (SPEC-0009 BR1: an appointment binds to the active beneficiary within
   * the caller's family scope). Added for the appointment module the way {@link #cardDetailsFor}
   * was added for the digital-card feature, because {@link #requireInScope} — which returns the
   * entity — is package-private and must not leak across the module boundary. Only active
   * beneficiaries are bookable, so an inactive dependent is treated as out of scope.
   *
   * @throws BeneficiaryNotAccessibleException when the caller card is absent/unknown or the target
   *     falls outside the caller's active family scope (existence never revealed, BR2).
   */
  public AccessibleBeneficiary requireAccessible(String beneficiaryCard, UUID targetBeneficiaryId) {
    return toAccessible(requireInScope(beneficiaryCard, targetBeneficiaryId));
  }

  /**
   * Whether the caller's plan carries the reimbursement right (SPEC-0015 BR1). Fail-closed: an
   * absent/unknown card is not eligible — mirrors {@link #accessibleFor}'s empty-list behavior
   * rather than throwing, since the reimbursement hub gate itself decides what to render.
   */
  public boolean reimbursementEligible(String beneficiaryCard) {
    return caller(beneficiaryCard)
        .map(beneficiary -> beneficiary.getPlan().isReimbursement())
        .orElse(false);
  }

  /**
   * Whether the scoped target beneficiary has the mandatory contact e-mail and mobile needed to
   * open a reimbursement request (SPEC-0015 BR2 via SPEC-0006 BR6). Out-of-scope targets still
   * throw {@link BeneficiaryNotAccessibleException}; missing contact data returns {@code false} so
   * the reimbursement module can raise its own BR2 error code.
   */
  public boolean hasRequiredContacts(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary target = requireInScope(beneficiaryCard, targetBeneficiaryId);
    ContactInfo contact = target.getContact();
    return contact != null
        && contact.getContactEmail() != null
        && !contact.getContactEmail().isBlank()
        && contact.getMobile() != null
        && !contact.getMobile().isBlank();
  }

  /**
   * The avatar URL for a beneficiary (SPEC-0006 BR3): the photo endpoint when a photo exists, else
   * {@code null} so the client shows the placeholder.
   */
  String avatarUrlFor(UUID beneficiaryId) {
    return photos.existsByBeneficiaryId(beneficiaryId)
        ? "/api/beneficiaries/" + beneficiaryId + "/photo"
        : null;
  }

  /**
   * The digital card + data sheet of {@code targetBeneficiaryId} within the caller's family scope
   * (SPEC-0007). Unlike {@link #summaryFor}, this variant does NOT exclude an inactive dependent
   * from the caller's scope: the card feature must tell "out of scope" ({@link
   * BeneficiaryNotAccessibleException}, 404) apart from "in scope but inactive" ({@code active =
   * false}, mapped to 409 {@code card.unavailable} by the card module — BR10) — excluding inactive
   * beneficiaries here the way {@link #accessibleEntities} does would collapse both into the same
   * 404, losing that distinction.
   *
   * @throws BeneficiaryNotAccessibleException when the caller card is absent/unknown or the target
   *     falls outside the caller's family scope (existence never revealed, BR2).
   */
  public CardDetails cardDetailsFor(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary callerBeneficiary =
        caller(beneficiaryCard).orElseThrow(BeneficiaryNotAccessibleException::new);
    Beneficiary target =
        familyRegardlessOfActive(callerBeneficiary).stream()
            .filter(beneficiary -> beneficiary.getId().equals(targetBeneficiaryId))
            .findFirst()
            .orElseThrow(BeneficiaryNotAccessibleException::new);
    return toCardDetails(target, !target.getId().equals(callerBeneficiary.getId()));
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

  private List<Beneficiary> familyRegardlessOfActive(Beneficiary caller) {
    List<Beneficiary> family = new ArrayList<>();
    family.add(caller);
    if (caller.getRole() == BeneficiaryRole.TITULAR) {
      family.addAll(beneficiaries.findByTitularIdOrderByBirthDate(caller.getId()));
    }
    return family;
  }

  private static AccessibleBeneficiary toAccessible(Beneficiary beneficiary) {
    return new AccessibleBeneficiary(
        beneficiary.getId(), firstName(beneficiary.getFullName()), beneficiary.getRole());
  }

  private static BeneficiarySummary toSummary(Beneficiary beneficiary, String avatarUrl) {
    return new BeneficiarySummary(
        beneficiary.getId(),
        firstName(beneficiary.getFullName()),
        beneficiary.getFullName(),
        beneficiary.getRole(),
        beneficiary.getPlan().getName(),
        beneficiary.getCardNumber(),
        avatarUrl);
  }

  private static CardDetails toCardDetails(Beneficiary beneficiary, boolean viewedAsDependent) {
    Plan plan = beneficiary.getPlan();
    return new CardDetails(
        beneficiary.getFullName(),
        beneficiary.getCardNumber(),
        beneficiary.getCns(),
        beneficiary.isActive(),
        viewedAsDependent,
        plan.getName(),
        plan.getAnsRegistration(),
        plan.getCoverage(),
        plan.getCategory(),
        plan.getAdditives());
  }

  private static String firstName(String fullName) {
    return fullName.strip().split("\\s+", 2)[0];
  }
}
