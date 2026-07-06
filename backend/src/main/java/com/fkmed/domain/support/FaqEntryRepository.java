package com.fkmed.domain.support;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link FaqEntry}; internal to the support module (DECISIONS-BASELINE §0016). */
@ModuleInternal
public interface FaqEntryRepository extends JpaRepository<FaqEntry, UUID> {

  /**
   * Active entries in content-defined order (SPEC-0014 BR5/BR6); category/query filtered in Java.
   */
  List<FaqEntry> findByActiveTrueOrderByDisplayOrderAsc();
}
