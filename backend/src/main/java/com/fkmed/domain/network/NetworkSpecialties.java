package com.fkmed.domain.network;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public facade of the network module's specialty registry (SPEC-0008 BR6). Exposes only {@link
 * SpecialtyOption} views, never the {@link Specialty} entity or its module-internal repository —
 * this is the ADR-0011 Wave 2 freeze point: from SPEC-0009 onward, {@code domain.appointment}
 * consumes this same facade (as a {@link SpecialtyValidator}) to validate appointment specialties
 * against the catalog network already seeds, instead of duplicating it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkSpecialties implements SpecialtyValidator {

  private final SpecialtyRepository specialties;

  @Override
  public boolean isValid(String code) {
    return code != null && specialties.existsById(code);
  }

  /** The full specialty catalog, alphabetical by name (BR6). */
  public List<SpecialtyOption> all() {
    return specialties.findAll(Sort.by("name")).stream()
        .map(s -> new SpecialtyOption(s.getCode(), s.getName()))
        .toList();
  }

  /** The names of the given specialty codes, alphabetical (used to render a provider's detail). */
  List<String> namesOf(Collection<String> codes) {
    if (codes.isEmpty()) {
      return List.of();
    }
    return specialties.findAllById(codes).stream().map(Specialty::getName).sorted().toList();
  }
}
