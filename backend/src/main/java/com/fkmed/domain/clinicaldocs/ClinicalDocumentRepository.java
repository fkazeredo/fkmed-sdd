package com.fkmed.domain.clinicaldocs;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Command-and-read repository of the clinical-document aggregate. The period filter's calendar
 * range is resolved to an {@code [from, toExclusive)} instant window by the service (product
 * timezone, DL-0019); the {@code category} filter is applied in-memory over the (already
 * beneficiary+period bounded) result — POC scale, mirroring {@code AppointmentService}'s
 * upcoming/history split.
 */
public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, UUID> {

  List<ClinicalDocument>
      findByBeneficiaryIdInAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
          Collection<UUID> beneficiaryIds, Instant from, Instant toExclusive);
}
