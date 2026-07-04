package com.fkmed.domain.plan;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service of the plan module: resolves the "my plan" view (SPEC-0001 BR6). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

  private final BeneficiaryRepository beneficiaries;

  /**
   * Resolves the plan and family members of the beneficiary holding {@code cardNumber}.
   *
   * <p>The member list starts with the titular, followed by the dependents linked to them.
   *
   * @param cardNumber the beneficiary card bound to the authenticated user; may be {@code null}
   *     when the user has no beneficiary link.
   * @throws PlanNotFoundException when the card is absent or matches no active beneficiary.
   */
  public MyPlanResponse myPlanFor(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      throw new PlanNotFoundException();
    }
    Beneficiary beneficiary =
        beneficiaries
            .findByCardNumberAndActiveTrue(cardNumber)
            .orElseThrow(PlanNotFoundException::new);
    Beneficiary titular =
        beneficiary.getRole() == BeneficiaryRole.TITULAR ? beneficiary : beneficiary.getTitular();
    List<Beneficiary> members = new ArrayList<>();
    members.add(titular);
    members.addAll(beneficiaries.findByTitularIdAndActiveTrueOrderByBirthDate(titular.getId()));
    return MyPlanResponse.from(titular.getPlan(), members);
  }
}
