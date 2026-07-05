package com.fkmed.domain.plan;

/**
 * Registry-validator port for the federative unit (UF) reference data (baseline §0019). Kept as a
 * small domain interface so contact validation stays testable without the persistence-backed
 * registry, and so the {@link ContactInfo} value object never imports a Spring service.
 */
@FunctionalInterface
public interface UfValidator {

  /** Whether {@code uf} (2-letter code, already upper-cased) exists in the UF registry. */
  boolean isValid(String uf);
}
