package com.fkmed.domain.plan;

import java.util.UUID;

/**
 * Selector view of a beneficiary the authenticated caller may act for (SPEC-0003 BR5). Carries no
 * CPF/CNS/bank data (BR8 masking norm); the identifier is a UUID so no personal data travels in
 * URLs.
 */
public record AccessibleBeneficiary(UUID beneficiaryId, String firstName, BeneficiaryRole role) {}
