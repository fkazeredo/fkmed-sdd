package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The copay write facade (SPEC-0013 BR8) — the ONLY way a copay charge is created. The single
 * caller is the operator simulation (SPEC-0018); the portal is read-only over copay entries.
 */
@Service
@RequiredArgsConstructor
public class Copays {

  private final CopayEntryRepository entries;

  /**
   * Records a copay charge for a family member's usage.
   *
   * @return the new entry's id.
   * @throws IllegalArgumentException when a required field is missing or the amount is not positive
   *     — an internal-contract violation by the calling sim.
   */
  @Transactional
  public UUID record(
      LocalDate entryDate,
      String procedure,
      String provider,
      UUID beneficiaryId,
      BigDecimal amount) {
    CopayEntry entry = CopayEntry.record(entryDate, procedure, provider, beneficiaryId, amount);
    entries.save(entry);
    return entry.getId();
  }
}
