/**
 * The guides-and-tokens module (SPEC-0012 — Guias e Token): transparency over authorization guides
 * opened by providers with the operator, and the short-lived beneficiary attendance token.
 *
 * <p>Owns {@code guide} (header + derived status, SPEC-0012 BR6) with its {@code guide_item}
 * children, and {@code attendance_token} (SPEC-0012 BR9-BR12) — two independent aggregates sharing
 * one module because both are read-mostly, beneficiary-facing views over data the operator moves
 * (SPEC-0018 in Wave 2). Guide transitions are driven ONLY through {@link
 * com.fkmed.domain.guides.GuideService}, called by the operator simulation (SPEC-0018); no
 * beneficiary write path exists for guides. Family scope reuses {@code domain.plan}'s {@link
 * com.fkmed.domain.plan.BeneficiaryAccess} (ADR-0013 precedent, mirroring {@code
 * domain.clinicaldocs}); guide numbers reuse {@code domain.plan}'s {@link
 * com.fkmed.domain.plan.ProtocolGenerator}.
 *
 * <p>Module map: ADR-0018 (12th verified module).
 */
@org.springframework.modulith.ApplicationModule(displayName = "guides")
package com.fkmed.domain.guides;
