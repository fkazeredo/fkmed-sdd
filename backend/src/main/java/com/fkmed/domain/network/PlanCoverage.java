package com.fkmed.domain.network;

import java.util.Optional;

/**
 * The current beneficiary's plan coverage for network search (SPEC-0008 BR4, DL-0014): {@code
 * coverageUf == null} models a NACIONAL plan (every UF allowed); a non-null value restricts the
 * funnel to that single UF (ESTADUAL). {@link #NONE} models the absence of a resolvable plan (no
 * beneficiary link, or an unknown card) — it allows no UF at all, never a silent "everything"
 * default.
 */
record PlanCoverage(String coverageUf) {

  /** No plan could be resolved for the caller — denies every UF. */
  static final PlanCoverage NONE = new PlanCoverage("¬none¬");

  /** Whether {@code uf} is within this coverage (BR4). */
  boolean allowsUf(String uf) {
    if (this == NONE) {
      return false;
    }
    return coverageUf == null || (uf != null && coverageUf.equalsIgnoreCase(uf));
  }

  /** Whether this is a NACIONAL coverage (every UF allowed). */
  boolean allowsEveryUf() {
    return this != NONE && coverageUf == null;
  }

  /** The single covered UF, when this is an ESTADUAL coverage; empty for NACIONAL/NONE. */
  Optional<String> singleUf() {
    return this == NONE || coverageUf == null ? Optional.empty() : Optional.of(coverageUf);
  }
}
