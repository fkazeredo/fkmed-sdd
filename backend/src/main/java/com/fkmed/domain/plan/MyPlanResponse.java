package com.fkmed.domain.plan;

import java.util.List;

/**
 * View returned by {@link PlanService#myPlanFor(String)} and rendered by the Meu Plano screen
 * (SPEC-0001 BR6). Response records that map an entity stay inside their domain module; the
 * delivery layer is entity-free.
 */
public record MyPlanResponse(PlanSummary plan, List<MemberSummary> members) {

  /** Plan data of the logged-in beneficiary's contract (SPEC-0001 BR4). */
  public record PlanSummary(
      String name,
      String ansRegistration,
      String coverage,
      boolean copay,
      boolean reimbursement,
      List<String> additives) {}

  /** A family member covered by the contract (SPEC-0001 BR5). */
  public record MemberSummary(String fullName, BeneficiaryRole role, String cardNumber) {}

  /** Maps the plan and the resolved family members (titular first) into the view. */
  public static MyPlanResponse from(Plan plan, List<Beneficiary> members) {
    return new MyPlanResponse(
        new PlanSummary(
            plan.getName(),
            plan.getAnsRegistration(),
            plan.getCoverage(),
            plan.isCopay(),
            plan.isReimbursement(),
            plan.getAdditives()),
        members.stream()
            .map(m -> new MemberSummary(m.getFullName(), m.getRole(), m.getCardNumber()))
            .toList());
  }
}
