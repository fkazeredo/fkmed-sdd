package com.fkmed.domain.support;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository of FAQ questions (SPEC-0014 BR5/BR6). Search/category filtering is applied by {@link
 * SupportService} over the active set (POC scale, mirroring {@code ClinicalDocumentRepository}'s
 * in-service filtering).
 */
public interface FaqEntryRepository extends JpaRepository<FaqEntry, UUID> {

  List<FaqEntry> findByActiveTrueOrderByDisplayOrderAsc();
}
