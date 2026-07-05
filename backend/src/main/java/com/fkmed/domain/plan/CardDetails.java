package com.fkmed.domain.plan;

import java.util.List;

/**
 * Full digital-card projection of a beneficiary, exposed by {@link
 * BeneficiaryAccess#cardDetailsFor} (SPEC-0007). Includes CNS in full — the sole BR8 exception (CNS
 * is shown in full only on the digital-card screen and its PDF); every other cross-module view in
 * this package (e.g. {@link BeneficiarySummary}, {@link AccessibleBeneficiary}) deliberately omits
 * it.
 *
 * @param active whether the beneficiary is currently active in the plan (BR10: inactive means the
 *     card is unavailable — 409 — as opposed to out of scope — 404).
 * @param viewedAsDependent whether this access is a titular viewing someone other than themselves
 *     (always a dependent, since the family-scope matrix never lets a caller reach anyone else) —
 *     the digital-card module audits exactly this case (BR7).
 */
public record CardDetails(
    String fullName,
    String cardNumber,
    String cns,
    boolean active,
    boolean viewedAsDependent,
    String planName,
    String ansRegistration,
    String coverage,
    String planCategory,
    List<String> additives) {}
