package com.fkmed.domain.finance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Command-and-read repository of copay charges (SPEC-0013 BR5). */
public interface CopayEntryRepository extends JpaRepository<CopayEntry, UUID> {

  /**
   * The family's copay charges within {@code [from, to]} for the given beneficiaries, most recent
   * first — the statement's period/beneficiary filter (BR5).
   */
  List<CopayEntry> findByBeneficiaryIdInAndEntryDateBetweenOrderByEntryDateDesc(
      Collection<UUID> beneficiaryIds, LocalDate from, LocalDate to);
}
