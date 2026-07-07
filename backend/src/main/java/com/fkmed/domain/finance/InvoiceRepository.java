package com.fkmed.domain.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Command-and-read repository of the invoice aggregate. Ordering per tab (BR2) and the derived
 * status are applied by {@link FinanceService} over the titular-scoped result (POC scale, mirroring
 * {@code ClinicalDocumentRepository}'s in-service filtering).
 */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  /** Every invoice of a titular's contract (unordered — the service orders per tab). */
  List<Invoice> findByTitularBeneficiaryId(UUID titularBeneficiaryId);

  /**
   * The invoice whose canonical digitable line matches (BR4 antifraud lookup, across all issued
   * invoices — an authenticity check, not a scope-bound read).
   */
  Optional<Invoice> findByDigitableLine(String digitableLine);
}
