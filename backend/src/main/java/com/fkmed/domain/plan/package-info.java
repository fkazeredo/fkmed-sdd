/**
 * The plan module: health plan contract and its beneficiaries (SPEC-0001).
 *
 * <p>Owns the {@code plan} and {@code beneficiary} tables (Flyway V1) and exposes the "my plan"
 * view consumed by the Meu Plano screen. Module map documented in ADR-0001.
 */
@org.springframework.modulith.ApplicationModule(displayName = "plan")
package com.fkmed.domain.plan;
