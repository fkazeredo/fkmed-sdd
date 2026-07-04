package com.fkmed.application.api;

import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiarySummary;
import com.fkmed.infra.security.UserContextProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Active-beneficiary context endpoints (SPEC-0003): the selector data source and the scope-checked
 * per-beneficiary card summary. Family-scope authorization is enforced server-side by {@link
 * BeneficiaryAccess} against the caller's beneficiary card claim — the client's active-beneficiary
 * choice is convenience, never authority (BR3).
 */
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextController {

  private final BeneficiaryAccess beneficiaryAccess;
  private final UserContextProvider userContext;

  /** Beneficiaries the caller may act for — selector source (SPEC-0003 BR5). */
  @GetMapping("/accessible-beneficiaries")
  List<AccessibleBeneficiary> accessibleBeneficiaries() {
    return beneficiaryAccess.accessibleFor(userContext.current().beneficiaryCard().orElse(null));
  }

  /** Card summary of a beneficiary within the caller's scope, else 404 (SPEC-0003 BR2/BR3). */
  @GetMapping("/beneficiaries/{beneficiaryId}")
  BeneficiarySummary beneficiary(@PathVariable UUID beneficiaryId) {
    return beneficiaryAccess.summaryFor(
        userContext.current().beneficiaryCard().orElse(null), beneficiaryId);
  }
}
