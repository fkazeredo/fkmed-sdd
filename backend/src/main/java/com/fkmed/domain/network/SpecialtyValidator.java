package com.fkmed.domain.network;

/**
 * Registry-validator port for the specialty catalog (baseline §0019), analogous to {@code
 * domain.plan.UfValidator}. Public across modules: from SPEC-0009 onward, {@code
 * domain.appointment} consumes it to validate appointment specialties against the same catalog
 * {@code domain.network} seeds (ADR-0011 Wave 2 freeze) — a one-directional {@code appointment →
 * network} dependency, no cycle.
 */
@FunctionalInterface
public interface SpecialtyValidator {

  /** Whether {@code code} exists in the seeded specialty registry. */
  boolean isValid(String code);
}
