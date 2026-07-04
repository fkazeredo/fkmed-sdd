package com.fkmed.domain.plan;

/**
 * Contractual role of a beneficiary inside a plan contract.
 *
 * <p>Enum keep-criterion (DECISIONS-BASELINE §0019): this is a closed structural classification
 * fixed by ANS supplementary-health regulation (titular holds the contract; dependents are linked
 * to a titular), and the domain enforces the titular-linkage invariant by branching on it. It is
 * not registry data — new values cannot appear at runtime.
 */
public enum BeneficiaryRole {
  TITULAR,
  DEPENDENT
}
