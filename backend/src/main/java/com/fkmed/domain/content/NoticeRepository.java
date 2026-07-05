package com.fkmed.domain.content;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Notice}; internal to the content module (DECISIONS-BASELINE §0016). */
@ModuleInternal
public interface NoticeRepository extends JpaRepository<Notice, UUID> {

  /** Active notices in content-defined order (SPEC-0005 BR7). */
  List<Notice> findByActiveTrueOrderByDisplayOrderAsc();
}
