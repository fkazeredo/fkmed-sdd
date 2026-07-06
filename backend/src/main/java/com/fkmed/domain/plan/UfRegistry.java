package com.fkmed.domain.plan;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registry-backed {@link UfValidator} (baseline §0019): checks a UF code against the seeded {@code
 * uf_registry}. Not cached — UF validation runs only on the low-frequency contact save, so a
 * Caffeine cache would add moving parts without solving a real hot-path problem (Rule Zero).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UfRegistry implements UfValidator {

  private final UfRegistryRepository registry;

  @Override
  public boolean isValid(String uf) {
    return uf != null && registry.existsById(uf);
  }
}
