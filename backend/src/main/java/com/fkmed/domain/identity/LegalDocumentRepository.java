package com.fkmed.domain.identity;

import com.fkmed.domain.ModuleInternal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link LegalDocument}; internal to the identity module (§0016). */
@ModuleInternal
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

  /** The current version of a document type: the latest published one (SPEC-0006 BR8). */
  Optional<LegalDocument> findFirstByTypeOrderByPublishedAtDesc(String type);
}
