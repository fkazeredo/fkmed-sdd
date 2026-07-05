package com.fkmed.domain.identity;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link TermAcceptance}; internal to the identity module (§0016). */
@ModuleInternal
public interface TermAcceptanceRepository extends JpaRepository<TermAcceptance, UUID> {

  List<TermAcceptance> findByAccountId(UUID accountId);

  /** Whether the account already accepted this version of a document type (SPEC-0006 BR8). */
  boolean existsByAccountIdAndDocumentTypeAndVersion(
      UUID accountId, String documentType, String version);
}
